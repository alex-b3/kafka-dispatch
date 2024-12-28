package dev.lydtech.dispatch.handler;

import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.service.DispatchService;
import dev.lydtech.dispatch.util.TestEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.*;

class OrderCreatorHandlerTest {

    private OrderCreatorHandler handler;
    private DispatchService dispatchServiceMock;

    @BeforeEach
    void setup(){
        dispatchServiceMock = mock(DispatchService.class);
        handler = new OrderCreatorHandler(dispatchServiceMock);
    }


    @Test
    void listen_Success() throws Exception {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        handler.listen(0, key, testEvent);
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }

    @Test
    void listen_Service() throws Exception {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RuntimeException("Service failure")).when(dispatchServiceMock).process(key, testEvent);
        handler.listen(0, key, testEvent);
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }
}