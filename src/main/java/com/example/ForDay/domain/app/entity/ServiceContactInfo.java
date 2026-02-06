package com.example.ForDay.domain.app.entity;

import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_contact_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ServiceContactInfo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_id")
    private Integer infoId;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName; // 서비스명 (포데이)

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName; // 운영 주체 (데이앤)

    @Column(nullable = false, length = 100)
    private String email; // 이메일

    @Column(nullable = false, length = 50)
    private String representative; // 대표자명

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber; // 대표번호

    public void updateContact(String serviceName, String companyName, String email,
                              String representative, String contactNumber) {
        this.serviceName = serviceName;
        this.companyName = companyName;
        this.email = email;
        this.representative = representative;
        this.contactNumber = contactNumber;
    }
}