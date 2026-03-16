package com.barinventory.invoice.service;

import com.barinventory.invoice.dto.ExtractedInvoiceData;

public interface InvoiceParserService {
	ExtractedInvoiceData parse(String rawText);

}
