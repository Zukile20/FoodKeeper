package com.example.foodkeeper.Recipe.Listeners;



import com.example.foodkeeper.Recipe.Models.InstructionsResponse;

import java.util.List;

public interface InstructionsListener {

    void didFetch(List<InstructionsResponse> response, String message);
    void didError(String message);
}
