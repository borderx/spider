package com.borderx.bean;

import java.io.Serializable;

/**
 * Created by borderx on 2018/2/10.
 */
public class Statistics implements Serializable {
    private int total;
    private int success;
    private int fail;
    private int codeError;

    public Statistics() {
        this.total = 0;
        this.success = 0;
        this.fail = 0;
        this.codeError = 0;
    }

    public void failIncrease() {
        fail ++;
    }

    public void totalIncrease() {
        total ++;
    }

    public void successIncrease() {
        success ++;
    }

    public void codeErrorIncrease() {
        codeError ++;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFail() {
        return fail;
    }

    public void setFail(int fail) {
        this.fail = fail;
    }

    public int getCodeError() {
        return codeError;
    }

    public void setCodeError(int codeError) {
        this.codeError = codeError;
    }
}
