package com.fastcampus.pass.repository.packaze;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PackageRepositoryTest {

    @Autowired
    private PackageRepository packageRepository;

    @Test
    @DisplayName("엔티티 저장 테스트")
    public void test_save(){
        //given
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setPackageName("바디 챌린지 PT 12주");
        packageEntity.setPeriod(84);
        //when
        packageRepository.save(packageEntity);
        //then
        assertNotNull(packageEntity.getPackageSeq());
    }

    @Test
    @DisplayName("엔티티 조회 테스트")
    public void test_findByCreatedAtAfter(){
        //given
        // 1분 전 시간을 가져오고, 해당 시간 이후에 생성된 아래의 객체들만 조회하는 테스트
        LocalDateTime dateTime = LocalDateTime.now().minusMinutes(1);

        PackageEntity packageEntity0 = new PackageEntity();
        packageEntity0.setPackageName("학생 전용 3개월");
        packageEntity0.setPeriod(90);
        packageRepository.save(packageEntity0);

        PackageEntity packageEntity1 = new PackageEntity();
        packageEntity1.setPackageName("학생 전용 6개월");
        packageEntity1.setPeriod(180);
        packageRepository.save(packageEntity1);

        //when
        List<PackageEntity> packageEntities = packageRepository.findByCreatedAtAfter(dateTime, PageRequest.of(0, 1, Sort.by("packageSeq").descending()));

        //then
        assertEquals(1, packageEntities.size());
        assertEquals(packageEntity1.getPackageSeq(), packageEntities.get(0).getPackageSeq());

    }

    @Test
    @DisplayName("엔티티 업데이트 테스트")
    public void test_updateCountAndPeriod(){
        // given
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setPackageName("바디프로필 이벤트 4개월");
        packageEntity.setPeriod(90);
        packageRepository.save(packageEntity);

        // when
        // jpql 사용하기
        int updatedCount = packageRepository.updateCountAndPeriod(packageEntity.getPackageSeq(), 30, 120);
        PackageEntity updatedPackageEntity = packageRepository.findById(packageEntity.getPackageSeq()).get();

        // then
        assertEquals(1, updatedCount);
        assertEquals(30, updatedPackageEntity.getCount());
        assertEquals(120, updatedPackageEntity.getPeriod());
    }

    @Test
    @DisplayName("엔티티 삭제 테스트")
    public void test_delete(){
        // given
        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setPackageName("제거할 이용권");
        packageEntity.setPeriod(1);
        PackageEntity newPackage = packageRepository.save(packageEntity);

        // when
        packageRepository.deleteById(newPackage.getPackageSeq());
        // then
        assertTrue(packageRepository.findById(packageEntity.getPackageSeq()).isEmpty());
    }
}