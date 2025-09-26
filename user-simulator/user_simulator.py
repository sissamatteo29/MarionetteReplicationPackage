#!/usr/bin/env python3
"""
Enhanced User Behavior Simulator for Outfit-App
===============================================

A simulator that:
1. Fetches gallery HTML pages
2. Automatically downloads all images on each page (like a real browser)
3. Tracks both HTML and image download performance
4. Simulates realistic user browsing behavior
5. Supports multiple concurrent fake users with threaded simulation
6. Uses a parent thread for cleanup operations only

Usage:
  python user_simulator.py
  python user_simulator.py --images-folder /path/to/images --cycle-duration 300
  python user_simulator.py --num-users 5 --cycle-duration 300 --max-cycles 3
"""

import os
import time
import random
import requests
import argparse
from pathlib import Path
from urllib.parse import urljoin, urlparse
import re
import threading
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


class ConnectionPoolManager:
    """Manages a shared connection pool for all simulator instances."""
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls, pool_size=20, max_retries=3, backoff_factor=0.3):
        """Singleton pattern to ensure only one connection pool manager exists."""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(ConnectionPoolManager, cls).__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self, pool_size=20, max_retries=3, backoff_factor=0.3):
        """Initialize the connection pool manager (only once due to singleton)."""
        if self._initialized:
            return
            
        self.pool_size = pool_size
        self.max_retries = max_retries
        self.backoff_factor = backoff_factor
        
        # Create a shared session with optimized connection pooling
        self.session = self._create_optimized_session()
        self._initialized = True
        
        print(f"🔗 Connection Pool Initialized:")
        print(f"   Pool size: {pool_size} connections")
        print(f"   Max retries: {max_retries}")
        print(f"   Backoff factor: {backoff_factor}")
        print()
    
    def _create_optimized_session(self):
        """Create a requests session with optimized connection pooling."""
        session = requests.Session()
        
        # Configure retry strategy
        retry_strategy = Retry(
            total=self.max_retries,
            backoff_factor=self.backoff_factor,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["HEAD", "GET", "PUT", "DELETE", "OPTIONS", "TRACE", "POST"]
        )
        
        # Configure HTTP adapter with connection pooling
        adapter = HTTPAdapter(
            pool_connections=self.pool_size,  # Number of connection pools to cache
            pool_maxsize=self.pool_size,      # Maximum number of connections in each pool
            max_retries=retry_strategy,
            pool_block=False  # Don't block if pool is full, create new connection
        )
        
        # Mount the adapter for both HTTP and HTTPS
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        # Set reasonable timeouts and headers
        session.headers.update({
            'User-Agent': 'OutfitApp-Simulator/1.0',
            'Connection': 'keep-alive'
        })
        
        return session
    
    def get_session(self):
        """Get the shared session instance."""
        return self.session
    
    def close(self):
        """Close the shared session and clean up resources."""
        if hasattr(self, 'session'):
            self.session.close()
            print("🔗 Connection pool closed")
    
    @classmethod
    def reset_instance(cls):
        """Reset the singleton instance (useful for testing)."""
        with cls._lock:
            if cls._instance:
                cls._instance.close()
            cls._instance = None


class SharedStats:
    """Thread-safe shared statistics for all user simulators."""
    
    def __init__(self):
        self.lock = threading.Lock()
        self.data = {
            'cycles_completed': 0,
            'uploads_attempted': 0,
            'uploads_successful': 0,
            'browse_requests': 0,
            'total_actions': 0,
            'successful_uploads_in_cycle': 0,
            'total_images_in_repository': 0,
            # New image download stats
            'html_pages_fetched': 0,
            'images_downloaded': 0,
            'image_download_failures': 0,
            'total_html_download_time': 0.0,
            'total_image_download_time': 0.0,
            'bytes_downloaded': 0
        }
    
    def increment(self, key, value=1):
        """Thread-safe increment of a stat."""
        with self.lock:
            if key not in self.data:
                self.data[key] = 0
            self.data[key] += value
    
    def set(self, key, value):
        """Thread-safe set of a stat."""
        with self.lock:
            self.data[key] = value
    
    def get(self, key):
        """Thread-safe get of a stat."""
        with self.lock:
            return self.data.get(key, 0)
    
    def get_all(self):
        """Thread-safe get of all stats."""
        with self.lock:
            return self.data.copy()


class EnhancedOutfitSimulator:
    """Enhanced simulator that downloads images like a real browser."""
    
    def __init__(self, images_folder="./test-images", cycle_duration=10, user_id=None, shared_stats=None, pool_manager=None):
        # Hard-coded endpoints
        self.base_url = "http://192.168.49.2"
        self.upload_url = f"{self.base_url}/upload"
        self.gallery_url = f"{self.base_url}/"
        self.admin_clear_url = f"{self.base_url}/admin/clear-repository"
        
        # Configuration
        self.images_folder = Path(images_folder)
        self.cycle_duration = cycle_duration
        self.action_interval = 1
        self.user_id = user_id  # For identifying different user threads
        
        # Action preferences
        self.upload_chance = 0.25
        self.browse_chance = 0.75
        
        # Load available images
        self.available_images = self._find_images()
        
        # Use shared stats if provided, otherwise create local stats
        if shared_stats:
            self.shared_stats = shared_stats
            self.stats = None  # We'll use shared_stats instead
        else:
            self.shared_stats = None
            # Enhanced statistics (local mode for single user)
            self.stats = {
                'cycles_completed': 0,
                'uploads_attempted': 0,
                'uploads_successful': 0,
                'browse_requests': 0,
                'total_actions': 0,
                'successful_uploads_in_cycle': 0,
                'total_images_in_repository': 0,
                # New image download stats
                'html_pages_fetched': 0,
                'images_downloaded': 0,
                'image_download_failures': 0,
                'total_html_download_time': 0.0,
                'total_image_download_time': 0.0,
                'bytes_downloaded': 0
            }
        
        # Use shared connection pool or create a default one
        if pool_manager:
            self.pool_manager = pool_manager
        else:
            # For single-user mode, still use the shared pool for consistency
            self.pool_manager = ConnectionPoolManager()
        
        self.session = self.pool_manager.get_session()
        
        # Update statistics to include connection pool info
        if self.shared_stats:
            self.shared_stats.increment('connection_pool_requests')
            if self.pool_manager:
                # Store pool info (this will be overwritten by other threads, but that's okay)
                self.shared_stats.set('pool_size', self.pool_manager.pool_size)
                self.shared_stats.set('max_retries', self.pool_manager.max_retries)
        else:
            if 'connection_pool_requests' not in self.stats:
                self.stats['connection_pool_requests'] = 0
                self.stats['pool_size'] = self.pool_manager.pool_size if self.pool_manager else 0
                self.stats['max_retries'] = self.pool_manager.max_retries if self.pool_manager else 0
            self.stats['connection_pool_requests'] += 1
        
        user_prefix = f"[USER {self.user_id}] " if self.user_id is not None else ""
        print(f"🚀 {user_prefix}Enhanced Simulator Initialized")
        print(f"   📁 Images folder: {self.images_folder}")
        print(f"   📄 Cycle duration: {self.cycle_duration} seconds")
        print(f"   ⏱️ Action interval: {self.action_interval} seconds")
        print(f"   📸 Available images: {len(self.available_images)}")
        print(f"   🎯 Upload chance: {self.upload_chance*100}%")
        print(f"   👀 Browse chance: {self.browse_chance*100}%")
        print(f"   🌐 Will download all images on each page (browser simulation)")
        print(f"   🔗 Using shared connection pool")
        print()

    def _get_stat(self, key):
        """Get a statistic value (thread-safe if using shared stats)."""
        if self.shared_stats:
            return self.shared_stats.get(key)
        else:
            return self.stats[key]

    def _increment_stat(self, key, value=1):
        """Increment a statistic (thread-safe if using shared stats)."""
        if self.shared_stats:
            self.shared_stats.increment(key, value)
        else:
            self.stats[key] += value

    def _set_stat(self, key, value):
        """Set a statistic (thread-safe if using shared stats)."""
        if self.shared_stats:
            self.shared_stats.set(key, value)
        else:
            self.stats[key] = value

    def _get_all_stats(self):
        """Get all statistics (thread-safe if using shared stats)."""
        if self.shared_stats:
            return self.shared_stats.get_all()
        else:
            return self.stats.copy()

    def _find_images(self):
        """Find all image files in the images folder."""
        if not self.images_folder.exists():
            print(f"❌ ERROR: Images folder {self.images_folder} does not exist!")
            return []
        
        image_extensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp']
        images = []
        
        for ext in image_extensions:
            images.extend(self.images_folder.glob(f"*{ext}"))
            images.extend(self.images_folder.glob(f"*{ext.upper()}"))
        
        print(f"🔍 Found {len(images)} images in {self.images_folder}")
        if images:
            print(f"   Examples: {[img.name for img in images[:3]]}")
        
        return images

    def _extract_image_urls_from_html(self, html_content, base_url):
        """Extract all image URLs from HTML content."""
        try:
            soup = BeautifulSoup(html_content, 'html.parser')
            image_urls = []
            
            # Find all img tags
            for img_tag in soup.find_all('img'):
                src = img_tag.get('src')
                if src:
                    # Convert relative URLs to absolute URLs
                    absolute_url = urljoin(base_url, src)
                    image_urls.append(absolute_url)
            
            # Filter to only include images from our application
            # (exclude external CDN images like Bootstrap icons)
            app_image_urls = []
            for url in image_urls:
                parsed = urlparse(url)
                if parsed.netloc == urlparse(base_url).netloc:
                    app_image_urls.append(url)
            
            return app_image_urls
            
        except Exception as e:
            print(f"❌ Error parsing HTML for images: {e}")
            return []

    def _download_image(self, image_url, timeout=30):
        """Download a single image and return success status and size."""
        try:
            start_time = time.time()
            response = self.session.get(image_url, timeout=timeout)
            download_time = time.time() - start_time
            
            if response.status_code == 200:
                image_size = len(response.content)
                print(f"   ✅ Downloaded image: {image_url.split('/')[-1]} ({image_size} bytes, {download_time:.2f}s)")
                return True, image_size, download_time
            else:
                print(f"   ❌ Image download failed: {image_url} (status: {response.status_code})")
                return False, 0, download_time
                
        except Exception as e:
            print(f"   ❌ Image download error: {image_url} - {e}")
            return False, 0, 0

    def _upload_random_image(self):
        """Upload a random image from the collection."""
        if not self.available_images:
            print("❌ No images available to upload")
            return False
        
        image_path = random.choice(self.available_images)
        
        try:
            print(f"📤 Uploading: {image_path.name}")
            
            with open(image_path, 'rb') as img_file:
                files = {'file': (image_path.name, img_file, 'image/jpeg')}
                response = self.session.post(self.upload_url, files=files, timeout=60)
            
            success = response.status_code in [200, 302, 303]
            
            if success:
                print(f"✅ Upload successful: {image_path.name}")
                self._increment_stat('uploads_successful')
                self._increment_stat('successful_uploads_in_cycle')
                self._increment_stat('total_images_in_repository')
            else:
                print(f"❌ Upload failed: {image_path.name} (status: {response.status_code})")
            
            self._increment_stat('uploads_attempted')
            return success
            
        except Exception as e:
            print(f"❌ Upload error for {image_path.name}: {e}")
            self._increment_stat('uploads_attempted')
            return False

    def _browse_gallery_page_with_images(self):
        """Browse a gallery page and download all images on it (like a real browser)."""
        available_pages = self._calculate_available_pages()
        
        if not available_pages:
            print("👀 No pages available to browse (no images uploaded yet)")
            self._increment_stat('browse_requests')
            return True
        
        page = random.choice(available_pages)
        
        try:
            print(f"👀 Browsing gallery page: {page} (available: {available_pages})")
            
            # Step 1: Download the HTML page
            html_start_time = time.time()
            response = self.session.get(self.gallery_url, params={'page': page}, timeout=30)
            html_download_time = time.time() - html_start_time
            
            if response.status_code != 200:
                print(f"❌ HTML fetch failed: page {page} (status: {response.status_code})")
                self._increment_stat('browse_requests')
                return False
            
            html_size = len(response.content)
            print(f"✅ HTML downloaded: page {page} ({html_size} bytes, {html_download_time:.2f}s)")
            
            # Update HTML stats
            self._increment_stat('html_pages_fetched')
            self._increment_stat('total_html_download_time', html_download_time)
            self._increment_stat('bytes_downloaded', html_size)
            
            # Step 2: Extract and download all images (simulate browser behavior)
            image_urls = self._extract_image_urls_from_html(response.text, self.base_url)
            
            if image_urls:
                print(f"🖼️  Found {len(image_urls)} images to download...")
                
                for image_url in image_urls:
                    success, image_size, download_time = self._download_image(image_url)
                    
                    if success:
                        self._increment_stat('images_downloaded')
                        self._increment_stat('total_image_download_time', download_time)
                        self._increment_stat('bytes_downloaded', image_size)
                    else:
                        self._increment_stat('image_download_failures')
                
                total_download_time = html_download_time + self._get_stat('total_image_download_time')
                print(f"📊 Page complete: {len(image_urls)} images, total time: {total_download_time:.2f}s")
            else:
                print("📭 No images found on this page")
            
            self._increment_stat('browse_requests')
            return True
            
        except Exception as e:
            print(f"❌ Browse error for page {page}: {e}")
            self._increment_stat('browse_requests')
            return False

    def _clear_repository(self):
        """Clear the image repository."""
        try:
            print("🗑️ Clearing repository...")
            response = self.session.post(self.admin_clear_url, timeout=30)
            
            success = response.status_code == 200
            
            if success:
                print("✅ Repository cleared successfully")
                self._set_stat('total_images_in_repository', 0)
            else:
                print(f"❌ Repository clear failed (status: {response.status_code})")
            
            return success
            
        except Exception as e:
            print(f"❌ Repository clear error: {e}")
            return False

    def _choose_and_execute_action(self):
        """Choose and execute a single action (upload or browse with image downloads)."""
        self._increment_stat('total_actions')
        
        if random.random() < self.upload_chance:
            action_type = "UPLOAD"
            success = self._upload_random_image()
        else:
            action_type = "BROWSE+IMAGES"
            success = self._browse_gallery_page_with_images()
        
        status = "✅" if success else "❌"
        user_prefix = f"[USER {self.user_id}] " if self.user_id is not None else ""
        print(f"{status} {user_prefix}Action #{self._get_stat('total_actions')}: {action_type}")
        
        return success

    def _print_cycle_stats(self):
        """Print enhanced statistics including image download metrics."""
        stats = self._get_all_stats()
        
        upload_success_rate = 0
        if stats['uploads_attempted'] > 0:
            upload_success_rate = (stats['uploads_successful'] / stats['uploads_attempted']) * 100
        
        image_success_rate = 0
        total_images_attempted = stats['images_downloaded'] + stats['image_download_failures']
        if total_images_attempted > 0:
            image_success_rate = (stats['images_downloaded'] / total_images_attempted) * 100
        
        avg_html_time = 0
        if stats['html_pages_fetched'] > 0:
            avg_html_time = stats['total_html_download_time'] / stats['html_pages_fetched']
        
        avg_image_time = 0
        if stats['images_downloaded'] > 0:
            avg_image_time = stats['total_image_download_time'] / stats['images_downloaded']
        
        total_mb = stats['bytes_downloaded'] / (1024 * 1024)
        
        available_pages = self._calculate_available_pages()
        
        print()
        print("📊 ENHANCED CYCLE STATISTICS")
        print("-" * 40)
        print(f"Total actions: {stats['total_actions']}")
        print(f"Uploads attempted: {stats['uploads_attempted']}")
        print(f"Uploads successful: {stats['uploads_successful']} ({upload_success_rate:.1f}%)")
        print(f"Browse requests: {stats['browse_requests']}")
        print(f"Images in repository: {stats['total_images_in_repository']}")
        print()
        print("🌐 BROWSER SIMULATION STATS")
        print(f"HTML pages fetched: {stats['html_pages_fetched']}")
        print(f"Images downloaded: {stats['images_downloaded']}")
        print(f"Image download failures: {stats['image_download_failures']}")
        print(f"Image success rate: {image_success_rate:.1f}%")
        print(f"Total data downloaded: {total_mb:.2f} MB")
        print(f"Avg HTML download time: {avg_html_time:.2f}s")
        print(f"Avg image download time: {avg_image_time:.2f}s")
        print(f"Available gallery pages: {len(available_pages)} {available_pages if available_pages else '(none)'}")
        print()
        print("🔗 CONNECTION POOL STATS")
        print(f"Pool size: {stats.get('pool_size', 'N/A')}")
        print(f"Max retries: {stats.get('max_retries', 'N/A')}")
        print(f"Pool requests: {stats.get('connection_pool_requests', 0)}")
        print(f"Cycles completed: {stats['cycles_completed']}")
        print()

    def run_single_cycle(self):
        """Run a single cycle of enhanced user simulation."""
        cycle_number = self._get_stat('cycles_completed') + 1
        
        print(f"🔄 STARTING ENHANCED CYCLE #{cycle_number}")
        print(f"   Duration: {self.cycle_duration} seconds")
        print(f"   Expected actions: ~{int(self.cycle_duration / self.action_interval)}")
        print(f"   Mode: Full browser simulation (HTML + images)")
        print()
        
        cycle_start_time = time.time()
        cycle_end_time = cycle_start_time + self.cycle_duration
        
        actions_in_cycle = 0
        
        while time.time() < cycle_end_time:
            self._choose_and_execute_action()
            actions_in_cycle += 1
            
            time_left = cycle_end_time - time.time()
            if time_left > self.action_interval:
                print(f"⏳ Waiting {self.action_interval}s before next action...")
                time.sleep(self.action_interval)
            else:
                break
        
        print()
        print(f"🏁 CYCLE #{cycle_number} COMPLETED")
        print(f"   Actions in this cycle: {actions_in_cycle}")
        print(f"   Duration: {time.time() - cycle_start_time:.1f} seconds")
        
        self._clear_repository()
        self._set_stat('successful_uploads_in_cycle', 0)
        self._increment_stat('cycles_completed')
        self._print_cycle_stats()
        
        self._clear_repository()
        self._set_stat('successful_uploads_in_cycle', 0)
        self._increment_stat('cycles_completed')
        self._print_cycle_stats()

    def run_user_cycle(self, cycle_event, cleanup_event):
        """Run a single cycle for a user thread (no cleanup operations)."""
        cycle_number = self._get_stat('cycles_completed') + 1
        
        user_prefix = f"[USER {self.user_id}] "
        print(f"🔄 {user_prefix}STARTING ENHANCED CYCLE #{cycle_number}")
        print(f"   Duration: {self.cycle_duration} seconds")
        print(f"   Expected actions: ~{int(self.cycle_duration / self.action_interval)}")
        print(f"   Mode: Full browser simulation (HTML + images)")
        print()
        
        cycle_start_time = time.time()
        cycle_end_time = cycle_start_time + self.cycle_duration
        
        actions_in_cycle = 0
        
        while time.time() < cycle_end_time and not cleanup_event.is_set():
            self._choose_and_execute_action()
            actions_in_cycle += 1
            
            time_left = cycle_end_time - time.time()
            if time_left > self.action_interval and not cleanup_event.is_set():
                print(f"⏳ {user_prefix}Waiting {self.action_interval}s before next action...")
                time.sleep(self.action_interval)
            else:
                break
        
        print()
        print(f"🏁 {user_prefix}CYCLE #{cycle_number} COMPLETED")
        print(f"   Actions in this cycle: {actions_in_cycle}")
        print(f"   Duration: {time.time() - cycle_start_time:.1f} seconds")
        
        # Signal that this user has completed the cycle
        cycle_event.set()

    def run_cleanup_cycle(self):
        """Run cleanup operations only (for parent thread)."""
        print("🗑️ [PARENT] Performing cleanup operations...")
        self._clear_repository()
        self._set_stat('successful_uploads_in_cycle', 0)
        self._increment_stat('cycles_completed')
        self._print_cycle_stats()
        print("✅ [PARENT] Cleanup completed")

    def run_multi_user_simulation(self, num_users=3, max_cycles=None, pool_size=None, max_retries=3, backoff_factor=0.3):
        """Run simulation with multiple user threads and a parent cleanup thread."""
        print(f"🚀 STARTING MULTI-USER ENHANCED SIMULATION")
        print(f"   Number of users: {num_users}")
        if max_cycles:
            print(f"   Will run {max_cycles} cycles")
        else:
            print(f"   Will run indefinitely (Ctrl+C to stop)")
        print(f"   🌐 Full browser simulation enabled")
        print()
        
        # 🔥 NEW: Initial startup cleanup for single-user mode
        print(f"🧹 [STARTUP] Initial cleanup before simulation begins")
        self._clear_repository()
        self._set_stat('successful_uploads_in_cycle', 0)
        print(f"✅ [STARTUP] Initial cleanup completed")
        print()
        
        shared_stats = SharedStats()
        
        # Create shared connection pool for all users
        if pool_size is None:
            # Default pool size: at least 10, or 2x number of users, whichever is larger
            pool_size = max(10, num_users * 2)
        
        pool_manager = ConnectionPoolManager(
            pool_size=pool_size,
            max_retries=max_retries,
            backoff_factor=backoff_factor
        )
        
        try:
            cycle_count = 0
            
            while True:
                if max_cycles and cycle_count >= max_cycles:
                    print(f"🎯 Reached maximum cycles ({max_cycles})")
                    break

                # Create events for synchronization
                cycle_events = [threading.Event() for _ in range(num_users)]
                cleanup_event = threading.Event()
                
                # Create user threads
                user_threads = []
                for i in range(num_users):
                    user_simulator = EnhancedOutfitSimulator(
                        images_folder=self.images_folder,
                        cycle_duration=self.cycle_duration,
                        user_id=i+1,
                        shared_stats=shared_stats,
                        pool_manager=pool_manager  # Pass shared connection pool
                    )
                    
                    thread = threading.Thread(
                        target=user_simulator.run_user_cycle,
                        args=(cycle_events[i], cleanup_event),
                        name=f"User-{i+1}"
                    )
                    user_threads.append(thread)
                    thread.start()
                
                # Wait for all user threads to complete their cycles
                print("⏳ [PARENT] Waiting for all users to complete their cycles...")
                for event in cycle_events:
                    event.wait()
                
                # Signal cleanup to stop any remaining user activities
                cleanup_event.set()
                
                # Wait for all threads to finish
                for thread in user_threads:
                    thread.join()
                
                # Post
                print(f"🧹 [PARENT] Post-cycle cleanup for cycle #{cycle_count + 1}")
                post_cycle_simulator = EnhancedOutfitSimulator(
                    images_folder=self.images_folder,
                    cycle_duration=self.cycle_duration,
                    shared_stats=shared_stats,
                    pool_manager=pool_manager  # Pass shared connection pool
                )
                post_cycle_simulator.run_cleanup_cycle()
                cycle_count += 1
                
                print(" Brief pause (12s) between cycles...")
                time.sleep(12)
                
        except KeyboardInterrupt:
            print()
            print("🛑 Simulation stopped by user (Ctrl+C)")
            cleanup_event.set()
            for thread in user_threads:
                if thread.is_alive():
                    thread.join(timeout=2)
        
        except Exception as e:
            print(f"❌ Simulation error: {e}")
            cleanup_event.set()
        
        finally:
            # Clean up connection pool
            pool_manager.close()
            
            print()
            print("🏁 FINAL ENHANCED STATISTICS")
            print("=" * 50)
            # Use shared stats for final printout
            final_simulator = EnhancedOutfitSimulator(
                images_folder=self.images_folder,
                cycle_duration=self.cycle_duration,
                shared_stats=shared_stats
            )
            final_simulator._print_cycle_stats()
            print("✅ Enhanced simulation ended cleanly")

    def run_continuous(self, max_cycles=None):
        """Run continuous enhanced cycles until stopped."""
        print(f"🚀 STARTING ENHANCED CONTINUOUS SIMULATION")
        if max_cycles:
            print(f"   Will run {max_cycles} cycles")
        else:
            print(f"   Will run indefinitely (Ctrl+C to stop)")
        print(f"   🌐 Full browser simulation enabled")
        print()
        
        # 🔥 NEW: Initial startup cleanup for single-user mode
        print(f"🧹 [STARTUP] Initial cleanup before simulation begins")
        self._clear_repository()
        self._set_stat('successful_uploads_in_cycle', 0)
        print(f"✅ [STARTUP] Initial cleanup completed")
        print()
        
        try:
            cycle_count = 0
            
            while True:
                if max_cycles and cycle_count >= max_cycles:
                    print(f"🎯 Reached maximum cycles ({max_cycles})")
                    break
                
                self.run_single_cycle()
                cycle_count += 1
                
                print("## Brief pause of 12s between cycles...")
                time.sleep(12)
                
        except KeyboardInterrupt:
            print()
            print("🛑 Simulation stopped by user (Ctrl+C)")
        
        except Exception as e:
            print(f"❌ Simulation error: {e}")
        
        finally:
            print()
            print("🏁 FINAL ENHANCED STATISTICS")
            print("=" * 50)
            self._print_cycle_stats()
            print("✅ Enhanced simulation ended cleanly")

    def _calculate_available_pages(self):
        """Calculate available gallery pages based on uploaded images."""
        total_images = self._get_stat('total_images_in_repository')
        if total_images == 0:
            return []
        
        max_page = (total_images - 1) // 4
        return list(range(max_page + 1))


def main():
    """Enhanced command-line interface with connection pooling options."""
    parser = argparse.ArgumentParser(
        description="Enhanced User Behavior Simulator with Connection Pooling and Multi-User Support",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python user_simulator.py
  python user_simulator.py --images-folder ./my-images
  python user_simulator.py --cycle-duration 600 --max-cycles 3
  python user_simulator.py --num-users 5 --cycle-duration 300
  python user_simulator.py --num-users 10 --pool-size 30 --max-retries 5
        """
    )
    
    # Basic simulation options
    parser.add_argument('--images-folder', default='./test-images',
                       help='Folder containing test images (default: ./test-images)')
    parser.add_argument('--cycle-duration', type=int, default=10,
                       help='Duration of each cycle in seconds (default: 10)')
    parser.add_argument('--max-cycles', type=int,
                       help='Maximum number of cycles to run (default: unlimited)')
    parser.add_argument('--num-users', type=int, default=1,
                       help='Number of fake users to simulate (default: 1)')
    
    # Connection pooling options
    parser.add_argument('--pool-size', type=int,
                       help='Connection pool size (default: auto-calculated based on users)')
    parser.add_argument('--max-retries', type=int, default=3,
                       help='Maximum number of HTTP retries (default: 3)')
    parser.add_argument('--backoff-factor', type=float, default=0.3,
                       help='Backoff factor for retries (default: 0.3)')
    
    args = parser.parse_args()
    
    # Validation
    if not Path(args.images_folder).exists():
        print(f"❌ Error: Images folder '{args.images_folder}' does not exist")
        print("💡 Please create the folder and add some image files")
        return 1
    
    if args.num_users < 1:
        print(f"❌ Error: Number of users must be at least 1")
        return 1
    
    if args.pool_size and args.pool_size < 1:
        print(f"❌ Error: Pool size must be at least 1")
        return 1
    
    if args.max_retries < 0:
        print(f"❌ Error: Max retries cannot be negative")
        return 1
    
    if args.backoff_factor < 0:
        print(f"❌ Error: Backoff factor cannot be negative")
        return 1
    
    # Calculate pool size if not specified
    pool_size = args.pool_size
    if pool_size is None:
        pool_size = max(10, args.num_users * 2)
    
    # Display configuration
    print(f"🔧 CONFIGURATION")
    print(f"   Users: {args.num_users}")
    print(f"   Pool size: {pool_size}")
    print(f"   Max retries: {args.max_retries}")
    print(f"   Backoff factor: {args.backoff_factor}")
    print()
    
    # Create simulator with connection pooling
    if args.num_users == 1:
        # Single-user mode
        pool_manager = ConnectionPoolManager(
            pool_size=pool_size,
            max_retries=args.max_retries,
            backoff_factor=args.backoff_factor
        )
        
        simulator = EnhancedOutfitSimulator(
            images_folder=args.images_folder,
            cycle_duration=args.cycle_duration,
            pool_manager=pool_manager
        )
        
        try:
            print("🚀 Running in single-user mode with connection pooling")
            simulator.run_continuous(max_cycles=args.max_cycles)
        finally:
            pool_manager.close()
    else:
        # Multi-user mode (connection pool will be created inside)
        simulator = EnhancedOutfitSimulator(
            images_folder=args.images_folder,
            cycle_duration=args.cycle_duration
        )
        
        print(f"🚀 Running in multi-user mode with {args.num_users} users")
        simulator.run_multi_user_simulation(
            num_users=args.num_users, 
            max_cycles=args.max_cycles,
            pool_size=pool_size,
            max_retries=args.max_retries,
            backoff_factor=args.backoff_factor
        )
    
    return 0


if __name__ == "__main__":
    exit(main())
