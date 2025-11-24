package com.outbacksmp.richman;

import java.util.Optional;

public interface Challenge {
    
    String getName();
    Optional<WinnerRecord> evaluate();

}