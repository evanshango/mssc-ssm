package com.codewithevans.msscssm.services;

import com.codewithevans.msscssm.domain.Payment;
import com.codewithevans.msscssm.domain.PaymentEvent;
import com.codewithevans.msscssm.domain.PaymentState;
import com.codewithevans.msscssm.exceptions.NotFoundException;
import com.codewithevans.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    public static final String PAYMENT_HEADER_ID = "payment_id";

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState, PaymentEvent> factory;
    private final PaymentStateChangeInterceptor stateChangeInterceptor;

    @Override
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);
        sendEvent(paymentId, sm, PaymentEvent.PRE_AUTHORIZE);
        return sm;
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> authPayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);
        sendEvent(paymentId, sm, PaymentEvent.AUTH_APPROVED);
        return sm;
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);
        sendEvent(paymentId, sm, PaymentEvent.AUTH_DECLINED);
        return null;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent event){
        Message<PaymentEvent> msg = MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_HEADER_ID, paymentId)
                .build();
        sm.sendEvent(msg);
    }

    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId){
        var payment = paymentRepository.findById(paymentId).orElseThrow(NotFoundException::new);

        StateMachine<PaymentState, PaymentEvent> sm = factory.getStateMachine(Long.toString(payment.getId()));

        sm.stop();

        sm.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(stateChangeInterceptor);
            sma.resetStateMachine(new DefaultStateMachineContext<>(
                    payment.getState(), null, null, null)
            );
        });

        sm.start();

        return sm;
    }
}
