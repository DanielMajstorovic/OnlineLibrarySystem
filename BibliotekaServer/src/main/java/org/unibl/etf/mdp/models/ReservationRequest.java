package org.unibl.etf.mdp.models;

import java.util.List;

public class ReservationRequest {
    private String memberName;
    private List<String> titles;

    public ReservationRequest() {
    }

    public ReservationRequest(String memberName, List<String> titles) {
        this.memberName = memberName;
        this.titles = titles;
    }

    public String getMemberName() {
        return memberName;
    }
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public List<String> getTitles() {
        return titles;
    }
    public void setTitles(List<String> titles) {
        this.titles = titles;
    }
}
