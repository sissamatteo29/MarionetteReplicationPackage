package com.outfit.imagestore.usecases.inbound;

import java.io.IOException;
import java.util.List;

public interface FetchImageNamesUseCase {

    public List<String> execute() throws IOException;
    
}
