package com.example.ForDay.domain.record.type;

/**
 * 이슈 #421 - 기록 신고 심사의 운영 정책.
 *
 * <p>정책 문서를 따로 두지 않고 이 enum을 단일 출처로 삼는다. LLM 프롬프트의 정책 설명도 여기서
 * 만들어지므로, 기준을 바꾸면 판정 기준과 자동 조치 범위가 함께 바뀐다(문서와 코드가 어긋나지 않게).
 *
 * <p>{@code autoActionAllowed}가 false인 카테고리는 확신도가 높아도 자동으로 가리지 않고 사람이
 * 검토한다. 혐오·괴롭힘·폭력은 맥락(인용, 풍자, 취미 맥락의 격한 표현)에 따라 판단이 갈려 오판
 * 비용이 크기 때문이다.
 */
public enum ModerationCategory {

    SEXUAL("음란·선정", true,
            "성적 행위·신체 노출을 묘사하거나 성적 만남·성매매를 유도하는 내용"),
    SPAM_AD("광고·스팸", true,
            "취미 기록과 무관한 상품·서비스 홍보, 외부 링크·연락처·오픈채팅 유도, 같은 내용의 도배"),
    HATE_HARASSMENT("혐오·괴롭힘·욕설", false,
            "특정인·집단에 대한 비하, 모욕, 위협, 따돌림, 심한 욕설(초성·변형 표기 포함)"),
    VIOLENCE("폭력·위험", false,
            "자해·자살 암시나 방법, 타인에 대한 폭력 묘사·조장, 위험한 행위 권유"),
    PRIVACY("개인정보 노출", true,
            "본인 또는 타인의 전화번호·주소·주민번호·계좌번호 등 식별 가능한 개인정보 게시"),
    NONE("위반 아님", false,
            "위 항목에 해당하지 않음. 취미와 무관하다는 이유, 단순한 불호, 품질이 낮다는 이유는 위반이 아니다");

    private final String label;
    private final boolean autoActionAllowed;
    private final String definition;

    ModerationCategory(String label, boolean autoActionAllowed, String definition) {
        this.label = label;
        this.autoActionAllowed = autoActionAllowed;
        this.definition = definition;
    }

    public String label() {
        return label;
    }

    public boolean autoActionAllowed() {
        return autoActionAllowed;
    }

    public String definition() {
        return definition;
    }

    /** 프롬프트에 넣을 정책 설명. 카테고리 순서·문구를 바꾸면 평가셋으로 다시 측정한다. */
    public static String policyText() {
        StringBuilder sb = new StringBuilder();
        for (ModerationCategory c : values()) {
            sb.append("- ").append(c.name()).append(" (").append(c.label).append("): ").append(c.definition).append('\n');
        }
        return sb.toString();
    }
}
