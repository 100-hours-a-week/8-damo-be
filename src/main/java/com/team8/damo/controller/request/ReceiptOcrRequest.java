package com.team8.damo.controller.request;

import com.team8.damo.service.request.ReceiptOcrServiceRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ReceiptOcrRequest {

    @NotBlank(message = "영수증 URL은 필수입니다.")
    private String receiptUrl;

    public ReceiptOcrServiceRequest toServiceRequest() {
        return new ReceiptOcrServiceRequest(receiptUrl);
    }
}
