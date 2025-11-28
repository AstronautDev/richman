package com.outbacksmp.richman.api;

import com.outbacksmp.richman.WinnerRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RichManAPI {

    interface CurrentRichman {
        UUID uuid();
        String name();
        double balance();
        Instant since();
    }

    Optional<CurrentRichman> getCurrentRichman();

    Instant getNextSelectionTime();

    Optional<WinnerRecord> getLastWinner();

}
