package com.codewithevans.msscssm.services;

import com.codewithevans.msscssm.domain.Payment;
import com.codewithevans.msscssm.domain.PaymentEvent;
import com.codewithevans.msscssm.domain.PaymentState;
import com.codewithevans.msscssm.exceptions.NotFoundException;
import com.codewithevans.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentStateChangeInterceptor extends StateMachineInterceptorAdapter<PaymentState, PaymentEvent> {

    private final PaymentRepository paymentRepository;

    @Override
    public void preStateChange(
            State<PaymentState, PaymentEvent> state, Message<PaymentEvent> message, Transition<PaymentState,
            PaymentEvent> transition, StateMachine<PaymentState, PaymentEvent> stateMachine, StateMachine<PaymentState,
            PaymentEvent> rootStateMachine
    ) {
        Optional.ofNullable(message).flatMap(msg -> Optional.ofNullable(
                (Long) msg.getHeaders().getOrDefault(PaymentServiceImpl.PAYMENT_HEADER_ID, -1L)
        )).ifPresent(paymentId -> {
            Payment payment = paymentRepository.findById(paymentId).orElseThrow(NotFoundException::new);
            payment.setState(state.getId());
            paymentRepository.save(payment);
        });
    }
}
