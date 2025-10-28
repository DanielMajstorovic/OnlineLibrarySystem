package org.unibl.etf.mdp.models;

import java.util.List;

public class Reservation {

    private int id;
    private String memberName;
    private List<String> titles;
    private boolean approved = false;

    public Reservation() {
    }

    public Reservation(int id, String memberName, List<String> titles) {
        this.id = id;
        this.memberName = memberName;
        this.titles = titles;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
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

    public boolean isApproved() {
        return approved;
    }
    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", memberName='" + memberName + '\'' +
                ", titles=" + titles +
                ", approved=" + approved +
                '}';
    }
}
