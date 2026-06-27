package com.cybernode.ai.distributed_codeforge.account_service.service;


import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.CheckoutRequest;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.CheckoutResponse;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.PortalResponse;

public interface PaymentProcessor {

    CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request);

    PortalResponse openCustomerPortal();

    void processWebhook(String payload, String sigHeader);
}
