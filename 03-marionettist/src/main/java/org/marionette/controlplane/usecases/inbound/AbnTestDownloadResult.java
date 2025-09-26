package org.marionette.controlplane.usecases.inbound;

import org.marionette.controlplane.adapters.inbound.downloadresult.dto.AbnTestResultsDTO;

public record AbnTestDownloadResult (AbnTestResultsDTO dto) {}  // TODO: technical debt, need to have its own internal data structure for use case
