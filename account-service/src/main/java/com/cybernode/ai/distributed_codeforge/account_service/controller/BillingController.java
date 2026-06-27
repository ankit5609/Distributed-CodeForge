package com.cybernode.ai.distributed_codeforge.account_service.controller;


import com.cybernode.ai.distributed_codeforge.account_service.service.PaymentProcessor;
import com.cybernode.ai.distributed_codeforge.account_service.service.SubscriptionService;
import com.cybernode.ai.distributed_codeforge.account_service.dto.subscription.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final SubscriptionService subscriptionService;
    private final PaymentProcessor paymentProcessor;

    @GetMapping("/api/me/subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription(){
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription());
    }

    @PostMapping("/api/payments/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutResponse(
            @RequestBody CheckoutRequest request){
        return ResponseEntity.ok(paymentProcessor.createCheckoutSessionUrl(request));
    }

    @PostMapping("/api/payments/portal")
    public ResponseEntity<PortalResponse> openCustomerPortal(){
        return ResponseEntity.ok(paymentProcessor.openCustomerPortal());
    }

    @PostMapping("/webhooks/payment")
    public ResponseEntity<Void> handlePaymentWebhooks(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader){
        paymentProcessor.processWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
