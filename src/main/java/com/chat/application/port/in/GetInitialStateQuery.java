package com.chat.application.port.in;

import com.chat.application.dto.InitialState;
public interface GetInitialStateQuery {
    InitialState get(String token);
}