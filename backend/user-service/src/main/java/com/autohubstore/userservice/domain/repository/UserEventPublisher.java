package com.autohubstore.userservice.domain.repository;

import com.autohubstore.userservice.domain.event.UserCreatedEvent;

public interface UserEventPublisher {

    void publishUserCreated(UserCreatedEvent event);

}
