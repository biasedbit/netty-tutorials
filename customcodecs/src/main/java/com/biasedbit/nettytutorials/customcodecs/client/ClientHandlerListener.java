package com.biasedbit.nettytutorials.customcodecs.client;

import com.biasedbit.nettytutorials.customcodecs.common.Envelope;

public interface ClientHandlerListener {

    void messageReceived(Envelope message);
}
