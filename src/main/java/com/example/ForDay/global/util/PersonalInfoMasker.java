package com.example.ForDay.global.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이슈 #421 - 외부 LLM으로 보내는 텍스트에서 개인정보 패턴을 탐지하고 가린다.
 *
 * <p>신고 심사는 운영 개인정보처리방침에 외부 위탁·국외 이전 조항이 없어 내부 테스트로만 진행한다.
 * 그래도 "외부로 나가는 사용자 텍스트는 반드시 이 클래스를 거친다"는 경로를 먼저 고정한다.
 *
 * <p>마스킹하면 LLM은 "개인정보 노출" 위반을 스스로 판단할 수 없게 된다. 그래서 유형별 탐지 건수를
 * 함께 돌려주고, 호출부가 이를 "개인정보 패턴 N건 탐지"라는 사실(근거)로 LLM에 넘긴다 -
 * LLM이 판단하기 어렵거나 맡기면 안 되는 사실은 코드가 만든다는 원칙이다.
 *
 * <p>외부 I/O가 없는 순수 문자열 처리라 포트를 두지 않는다(docs/architecture-rules.md §6).
 */
public final class PersonalInfoMasker {

    public enum Type {
        RESIDENT_NUMBER("[주민번호]"),
        CARD_NUMBER("[카드번호]"),
        PHONE("[전화번호]"),
        EMAIL("[이메일]"),
        ACCOUNT_NUMBER("[계좌번호]");

        private final String placeholder;

        Type(String placeholder) {
            this.placeholder = placeholder;
        }

        public String placeholder() {
            return placeholder;
        }
    }

    public record Result(String masked, Map<Type, Integer> counts) {
        public boolean detected() {
            return !counts.isEmpty();
        }

        public int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private record Rule(Type type, Pattern pattern, int minDigits) {
    }

    // 순서가 중요하다: 더 구체적인 패턴(주민번호·카드번호)을 먼저 치환해야, 같은 숫자열이
    // 전화번호·계좌번호 같은 느슨한 패턴에 먼저 걸리지 않는다.
    private static final List<Rule> RULES = List.of(
            new Rule(Type.RESIDENT_NUMBER, Pattern.compile("(?<!\\d)\\d{6}\\s*-\\s*[1-8]\\d{6}(?!\\d)"), 13),
            new Rule(Type.CARD_NUMBER, Pattern.compile("(?<!\\d)\\d{4}(?:[\\s-]?\\d{4}){3}(?!\\d)"), 16),
            // 휴대폰: 구분자 없음/하이픈/점/공백, 국제 형식(+82), 숫자 사이를 띄워 쓴 회피 표기(0 1 0 ...)까지
            new Rule(Type.PHONE, Pattern.compile(
                    "(?:\\+82[\\s-]?|(?<!\\d)0\\s*)1\\s*[016789](?:[\\s.\\-]*\\d){7,8}(?!\\d)"), 10),
            // 유선: 02-123-4567, 031-1234-5678
            new Rule(Type.PHONE, Pattern.compile("(?<!\\d)0\\d{1,2}[\\s.\\-]\\d{3,4}[\\s.\\-]\\d{4}(?!\\d)"), 9),
            new Rule(Type.EMAIL, Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), 0),
            // 계좌: 하이픈으로 묶인 숫자 덩어리. 날짜(2026-11-18, 8자리)와 구분하려고 숫자 10자리 이상만 본다.
            new Rule(Type.ACCOUNT_NUMBER, Pattern.compile("(?<!\\d)\\d{2,6}-\\d{2,6}-\\d{2,8}(?:-\\d{1,4})?(?!\\d)"), 10)
    );

    private PersonalInfoMasker() {
    }

    public static Result mask(String text) {
        if (text == null || text.isBlank()) {
            return new Result(text, Collections.emptyMap());
        }
        Map<Type, Integer> counts = new EnumMap<>(Type.class);
        String current = text;
        for (Rule rule : RULES) {
            current = replace(current, rule, counts);
        }
        return new Result(current, Collections.unmodifiableMap(counts));
    }

    private static String replace(String text, Rule rule, Map<Type, Integer> counts) {
        Matcher matcher = rule.pattern().matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String found = matcher.group();
            if (digitCount(found) < rule.minDigits()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(found));
                continue;
            }
            counts.merge(rule.type(), 1, Integer::sum);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rule.type().placeholder()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static int digitCount(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                n++;
            }
        }
        return n;
    }
}
