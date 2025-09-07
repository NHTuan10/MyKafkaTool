package io.github.nhtuan10.mykafkatool.ui.codehighlighting;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class Match implements Comparable<Match> {
    public final String kind;
    public final int start;
    public final int end;

    @Override
    public int compareTo(Match o) {
        return Integer.compare(start, o.start);
    }
}
