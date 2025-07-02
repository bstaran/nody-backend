package org.nodystudio.nodybackend.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 위치 정보 처리를 위한 유틸리티 클래스
 */
@UtilityClass
public class LocationUtils {

  private static final double EARTH_RADIUS_KM = 6371.0; // 지구 반지름 (km)

  /**
   * Haversine 공식을 사용하여 두 좌표 간의 거리를 계산합니다.
   * 
   * @param lat1 첫 번째 지점의 위도
   * @param lon1 첫 번째 지점의 경도
   * @param lat2 두 번째 지점의 위도
   * @param lon2 두 번째 지점의 경도
   * @return 두 지점 간의 거리 (km)
   */
  public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1,
      BigDecimal lat2, BigDecimal lon2) {
    if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
      throw new IllegalArgumentException("위도와 경도는 null일 수 없습니다.");
    }

    // 위도/경도 유효성 검증
    validateLatitude(lat1);
    validateLongitude(lon1);
    validateLatitude(lat2);
    validateLongitude(lon2);

    double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
    double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1.doubleValue())) *
            Math.cos(Math.toRadians(lat2.doubleValue())) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = EARTH_RADIUS_KM * c;

    return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP);
  }

  /**
   * 위도의 유효성을 검증합니다.
   * 
   * @param latitude 검증할 위도 (-90 ~ 90)
   * @throws IllegalArgumentException 유효하지 않은 위도인 경우
   */
  public static void validateLatitude(BigDecimal latitude) {
    if (latitude == null) {
      throw new IllegalArgumentException("위도는 null일 수 없습니다.");
    }

    if (latitude.compareTo(BigDecimal.valueOf(-90.0)) < 0 ||
        latitude.compareTo(BigDecimal.valueOf(90.0)) > 0) {
      throw new IllegalArgumentException("위도는 -90도에서 90도 사이여야 합니다. 입력값: " + latitude);
    }
  }

  /**
   * 경도의 유효성을 검증합니다.
   * 
   * @param longitude 검증할 경도 (-180 ~ 180)
   * @throws IllegalArgumentException 유효하지 않은 경도인 경우
   */
  public static void validateLongitude(BigDecimal longitude) {
    if (longitude == null) {
      throw new IllegalArgumentException("경도는 null일 수 없습니다.");
    }

    if (longitude.compareTo(BigDecimal.valueOf(-180.0)) < 0 ||
        longitude.compareTo(BigDecimal.valueOf(180.0)) > 0) {
      throw new IllegalArgumentException("경도는 -180도에서 180도 사이여야 합니다. 입력값: " + longitude);
    }
  }

  /**
   * 위도/경도 좌표가 모두 유효한지 검증합니다.
   * 
   * @param latitude  위도
   * @param longitude 경도
   */
  public static void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
    validateLatitude(latitude);
    validateLongitude(longitude);
  }

  /**
   * 주소-좌표 변환 인터페이스 (추후 카카오맵 API 연동)
   * 현재는 인터페이스만 제공하고, 추후 외부 API 연동 시 구현 예정
   */
  public interface GeocodingService {

    /**
     * 주소를 좌표로 변환합니다.
     * 
     * @param address 변환할 주소
     * @return 위도/경도 좌표 객체
     * @throws GeocodingException 주소 변환 실패 시
     * @throws IllegalArgumentException 주소가 null이거나 비어있을 때
     */
    Coordinates addressToCoordinates(String address);

    /**
     * 좌표를 주소로 변환합니다.
     * 
     * @param latitude  위도
     * @param longitude 경도
     * @return 주소 문자열
     * @throws GeocodingException 좌표 변환 실패 시
     * @throws IllegalArgumentException 좌표가 유효하지 않을 때
     */
    String coordinatesToAddress(BigDecimal latitude, BigDecimal longitude);
  }

  /**
   * 좌표 정보를 담는 클래스
   */
  public static class Coordinates {
    private final BigDecimal latitude;
    private final BigDecimal longitude;

    public Coordinates(BigDecimal latitude, BigDecimal longitude) {
      validateCoordinates(latitude, longitude);
      this.latitude = latitude;
      this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
      return latitude;
    }

    public BigDecimal getLongitude() {
      return longitude;
    }
  }
}