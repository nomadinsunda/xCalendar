package com.intheeast.acalendar;

import android.accounts.Account;
import android.content.Context;

public class RecipientAdapter {

    private final Context context;
    private Account account;

    public RecipientAdapter(Context context) {
        this.context = context;
    }

    /**
     * Set the account when known. Causes the search to prioritize contacts from
     * that account.
     */
    public void setAccount(Account account) {
        if (account != null) {
            // 이메일 계정 타입을 결정할 수 없는 경우 기본 타입 설정
            String accountType = determineAccountType(account);
            this.account = new Account(account.name, accountType);
        }
    }

    /**
     * Account 타입을 결정하는 메서드. 이메일에 기반하여 타입을 추론할 수 있습니다.
     */
    private String determineAccountType(Account account) {
        // 이메일에 따라 타입을 결정하는 로직 추가 가능
        // 예: 특정 도메인에 기반하여 타입 결정
        if (account.name.endsWith("@gmail.com")) {
            return "com.google";
        } else if (account.name.endsWith("@yahoo.com")) {
            return "com.yahoo";
        }
        // 기본값
        return "unknown";
    }

    public Account getAccount() {
        return account;
    }

    // 기타 필요한 메서드 및 기능 구현
}
