package org.nodystudio.nodybackend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * - Haversine 공식을 사용한 거리 계산 테스트
 * - 위도/경도 유효성 검증 테스트
 */
@DisplayName("LocationUtils 테스트")
class LocationUtilsTest {

  @Test
  @DisplayName("Haversine 공식을 사용한 거리 계산 테스트")
  void calculateDistance_HaversineFormula_Success() {
    // given - 서울 시청과 강남역 간의 거리
    BigDecimal seoulCityHallLat = new BigDecimal("37.5666805");
    BigDecimal seoulCityHallLon = new BigDecimal("126.9784147");
    BigDecimal gangnamStationLat = new BigDecimal("37.4979462");
    BigDecimal gangnamStationLon = new BigDecimal("127.0276368");

    // when
    BigDecimal distance = LocationUtils.calculateDistance(
        seoulCityHallLat, seoulCityHallLon,
        gangnamStationLat, gangnamStationLon);

    // then - 실제 거리는 약 8.8km
    assertThat(distance).isNotNull();
    assertThat(distance.doubleValue()).isBetween(8.5, 9.0);
  }

  @Test
  @DisplayName("동일한 좌표의 거리 계산 테스트")
  void calculateDistance_SameLocation_ReturnsZero() {
    // given
    BigDecimal lat = new BigDecimal("37.5665");
    BigDecimal lon = new BigDecimal("126.9780");

    // when
    BigDecimal distance = LocationUtils.calculateDistance(lat, lon, lat, lon);

    // then
    assertThat(distance.doubleValue()).isEqualTo(0.0, offset(0.001));
  }

  @Test
  @DisplayName("위도 유효성 검증 테스트 - 정상 범위")
  void validateLatitude_ValidRange_Success() {
    // given
    BigDecimal validLatitude1 = new BigDecimal("37.5665");
    BigDecimal validLatitude2 = new BigDecimal("-33.8688");
    BigDecimal validLatitude3 = new BigDecimal("90.0");
    BigDecimal validLatitude4 = new BigDecimal("-90.0");

    // when & then
    assertThatNoException().isThrownBy(() -> {
      LocationUtils.validateLatitude(validLatitude1);
      LocationUtils.validateLatitude(validLatitude2);
      LocationUtils.validateLatitude(validLatitude3);
      LocationUtils.validateLatitude(validLatitude4);
    });
  }

  @Test
  @DisplayName("위도 유효성 검증 테스트 - 범위 초과")
  void validateLatitude_OutOfRange_ThrowsException() {
    // given
    BigDecimal invalidLatitude1 = new BigDecimal("90.1");
    BigDecimal invalidLatitude2 = new BigDecimal("-90.1");
    BigDecimal invalidLatitude3 = new BigDecimal("180.0");

    // when & then
    assertThatThrownBy(() -> LocationUtils.validateLatitude(invalidLatitude1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도는 -90도에서 90도 사이여야 합니다. 입력값: 90.1");

    assertThatThrownBy(() -> LocationUtils.validateLatitude(invalidLatitude2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도는 -90도에서 90도 사이여야 합니다. 입력값: -90.1");

    assertThatThrownBy(() -> LocationUtils.validateLatitude(invalidLatitude3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도는 -90도에서 90도 사이여야 합니다. 입력값: 180.0");
  }

  @Test
  @DisplayName("경도 유효성 검증 테스트 - 정상 범위")
  void validateLongitude_ValidRange_Success() {
    // given
    BigDecimal validLongitude1 = new BigDecimal("126.9780");
    BigDecimal validLongitude2 = new BigDecimal("-74.0060");
    BigDecimal validLongitude3 = new BigDecimal("180.0");
    BigDecimal validLongitude4 = new BigDecimal("-180.0");

    // when & then
    assertThatNoException().isThrownBy(() -> {
      LocationUtils.validateLongitude(validLongitude1);
      LocationUtils.validateLongitude(validLongitude2);
      LocationUtils.validateLongitude(validLongitude3);
      LocationUtils.validateLongitude(validLongitude4);
    });
  }

  @Test
  @DisplayName("경도 유효성 검증 테스트 - 범위 초과")
  void validateLongitude_OutOfRange_ThrowsException() {
    // given
    BigDecimal invalidLongitude1 = new BigDecimal("180.1");
    BigDecimal invalidLongitude2 = new BigDecimal("-180.1");
    BigDecimal invalidLongitude3 = new BigDecimal("360.0");

    // when & then
    assertThatThrownBy(() -> LocationUtils.validateLongitude(invalidLongitude1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도는 -180도에서 180도 사이여야 합니다. 입력값: 180.1");

    assertThatThrownBy(() -> LocationUtils.validateLongitude(invalidLongitude2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도는 -180도에서 180도 사이여야 합니다. 입력값: -180.1");

    assertThatThrownBy(() -> LocationUtils.validateLongitude(invalidLongitude3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도는 -180도에서 180도 사이여야 합니다. 입력값: 360.0");
  }

  @Test
  @DisplayName("null 값 검증 테스트")
  void validate_NullValues_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> LocationUtils.validateLatitude(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도는 null일 수 없습니다.");

    assertThatThrownBy(() -> LocationUtils.validateLongitude(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도는 null일 수 없습니다.");

    assertThatThrownBy(() -> LocationUtils.calculateDistance(null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도와 경도는 null일 수 없습니다.");
  }

  @Test
  @DisplayName("좌표 통합 검증 테스트")
  void validateCoordinates_Success() {
    // given
    BigDecimal validLatitude = new BigDecimal("37.5665");
    BigDecimal validLongitude = new BigDecimal("126.9780");

    // when & then
    assertThatNoException().isThrownBy(() -> LocationUtils.validateCoordinates(validLatitude, validLongitude));
  }

  @Test
  @DisplayName("Coordinates 클래스 테스트")
  void coordinates_Creation_Success() {
    // given
    BigDecimal latitude = new BigDecimal("37.5665");
    BigDecimal longitude = new BigDecimal("126.9780");

    // when
    LocationUtils.Coordinates coordinates = new LocationUtils.Coordinates(latitude, longitude);

    // then
    assertThat(coordinates.getLatitude()).isEqualTo(latitude);
    assertThat(coordinates.getLongitude()).isEqualTo(longitude);
  }

  @Test
  @DisplayName("Coordinates 클래스 - 잘못된 좌표로 생성 시 예외 발생")
  void coordinates_InvalidValues_ThrowsException() {
    // given
    BigDecimal invalidLatitude = new BigDecimal("999.0");
    BigDecimal validLongitude = new BigDecimal("126.9780");

    // when & then
    assertThatThrownBy(() -> new LocationUtils.Coordinates(invalidLatitude, validLongitude))
        .isInstanceOf(IllegalArgumentException.class);
  }
}