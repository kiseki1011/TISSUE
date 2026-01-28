package com.tissue.member.adapter.in.web.request;

import jakarta.validation.constraints.Size;

public record WithdrawMemberRequest(@Size(max = 100) String password) {}
