package com.borderx.bean;

import java.io.Serializable;

/**
 * Created by borderx on 2018/2/8.
 */
public class Pet implements Serializable {

    private String id;
    private String petId;
    private int birthType;
    private int mutation;
    private int generation;
    private int rareDegree;
    private String desc;
    private int petType;
    private double amount;
    private String bgColor;
    private String petUrl;
    private String validCode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public int getBirthType() {
        return birthType;
    }

    public void setBirthType(int birthType) {
        this.birthType = birthType;
    }

    public int getMutation() {
        return mutation;
    }

    public void setMutation(int mutation) {
        this.mutation = mutation;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public int getRareDegree() {
        return rareDegree;
    }

    public void setRareDegree(int rareDegree) {
        this.rareDegree = rareDegree;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getPetType() {
        return petType;
    }

    public void setPetType(int petType) {
        this.petType = petType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getPetUrl() {
        return petUrl;
    }

    public void setPetUrl(String petUrl) {
        this.petUrl = petUrl;
    }

    public String getValidCode() {
        return validCode;
    }

    public void setValidCode(String validCode) {
        this.validCode = validCode;
    }
}
