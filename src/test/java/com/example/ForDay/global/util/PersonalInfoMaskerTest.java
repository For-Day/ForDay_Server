package com.example.ForDay.global.util;

import com.example.ForDay.global.util.PersonalInfoMasker.Result;
import com.example.ForDay.global.util.PersonalInfoMasker.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class PersonalInfoMaskerTest {

    @Nested
    @DisplayName("전화번호")
    class Phone {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
                "연락주세요 010-1234-5678",
                "연락주세요 01012345678",
                "연락주세요 010.1234.5678",
                "연락주세요 010 1234 5678",
                "연락주세요 +82 10-1234-5678",
                "연락주세요 0 1 0 1 2 3 4 5 6 7 8",
                "연락주세요 02-123-4567",
                "연락주세요 031-1234-5678"
        })
        @DisplayName("다양한 표기와 띄어쓰기 회피 표기를 가린다")
        void masksVariants(String text) {
            Result result = PersonalInfoMasker.mask(text);

            assertThat(result.masked()).isEqualTo("연락주세요 [전화번호]");
            assertThat(result.counts()).containsExactly(entry(Type.PHONE, 1));
        }
    }

    @Nested
    @DisplayName("주민번호·카드번호·계좌번호·이메일")
    class OtherTypes {

        @Test
        @DisplayName("주민번호는 전화번호·계좌번호보다 먼저 판별한다")
        void residentNumber() {
            Result result = PersonalInfoMasker.mask("주민번호 900101-1234567 입니다");

            assertThat(result.masked()).isEqualTo("주민번호 [주민번호] 입니다");
            assertThat(result.counts()).containsExactly(entry(Type.RESIDENT_NUMBER, 1));
        }

        @Test
        @DisplayName("16자리 카드번호를 가린다")
        void cardNumber() {
            Result result = PersonalInfoMasker.mask("카드 1234-5678-9012-3456 / 1234567890123456");

            assertThat(result.masked()).isEqualTo("카드 [카드번호] / [카드번호]");
            assertThat(result.counts()).containsExactly(entry(Type.CARD_NUMBER, 2));
        }

        @Test
        @DisplayName("하이픈으로 묶인 10자리 이상 숫자는 계좌번호로 본다")
        void accountNumber() {
            Result result = PersonalInfoMasker.mask("입금 110-123-456789 국민 123456-01-123456");

            assertThat(result.masked()).isEqualTo("입금 [계좌번호] 국민 [계좌번호]");
            assertThat(result.counts()).containsExactly(entry(Type.ACCOUNT_NUMBER, 2));
        }

        @Test
        @DisplayName("이메일을 가린다")
        void email() {
            Result result = PersonalInfoMasker.mask("메일 hobby.lover@forday.kr 로");

            assertThat(result.masked()).isEqualTo("메일 [이메일] 로");
        }

        @Test
        @DisplayName("여러 유형이 섞여 있으면 유형별로 센다")
        void mixed() {
            Result result = PersonalInfoMasker.mask("010-1111-2222 a@b.com 010-3333-4444");

            assertThat(result.counts()).containsOnly(entry(Type.PHONE, 2), entry(Type.EMAIL, 1));
            assertThat(result.total()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("오탐하지 않아야 하는 문장")
    class NoFalsePositive {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
                "2026-11-18 아침 러닝 5km 완료",
                "오늘 10시 30분부터 2시간 독서",
                "책 300쪽 중 120쪽 읽음",
                "기록 66일 중 12일째",
                "1234번 버스 타고 공원 산책"
        })
        @DisplayName("날짜·시간·일반 숫자는 그대로 둔다")
        void keepsOrdinaryNumbers(String text) {
            Result result = PersonalInfoMasker.mask(text);

            assertThat(result.masked()).isEqualTo(text);
            assertThat(result.detected()).isFalse();
        }

        @Test
        @DisplayName("null·빈 문자열은 그대로 돌려준다")
        void nullOrBlank() {
            assertThat(PersonalInfoMasker.mask(null).detected()).isFalse();
            assertThat(PersonalInfoMasker.mask("  ").masked()).isEqualTo("  ");
        }
    }
}
