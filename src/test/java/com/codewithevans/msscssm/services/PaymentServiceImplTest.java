package com.codewithevans.msscssm.services;

import com.codewithevans.msscssm.domain.Payment;
import com.codewithevans.msscssm.domain.PaymentEvent;
import com.codewithevans.msscssm.domain.PaymentState;
import com.codewithevans.msscssm.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentServiceImplTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    Payment payment;

    @BeforeEach
    void setUp(){
        payment = Payment.builder().amount(new BigDecimal("12.99")).build();
    }

    @Transactional
    @Test
    void preAuth() {
        Payment savedPayment = paymentService.newPayment(payment);

        paymentService.preAuth(savedPayment.getId());

        Payment preAuthedPayment = paymentRepository.getById(payment.getId());

        assertNotNull(preAuthedPayment);
        System.out.println(preAuthedPayment);
    }

    @Transactional
    @RepeatedTest(10)
    void testAuth(){
        Payment savedPayment = paymentService.newPayment(payment);

        StateMachine<PaymentState, PaymentEvent> preAuthSM = paymentService.preAuth(savedPayment.getId());

        if (preAuthSM.getState().getId() == PaymentState.PRE_AUTH){
            System.out.println("Payment is pre-authorized");
            StateMachine<PaymentState, PaymentEvent> authSM = paymentService.authPayment(savedPayment.getId());

            System.out.println("Result of auth: " + authSM.getState().getId());
        } else {
            System.out.println("Payment failed pre-auth...");
        }
    }
}