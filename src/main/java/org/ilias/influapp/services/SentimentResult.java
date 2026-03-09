package org.ilias.influapp.services;


public record SentimentResult(
        double polarity,

        String label,
        double confidence,

        int sentimentClass
)

    {
    }


