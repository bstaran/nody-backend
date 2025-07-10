package org.nodystudio.nodybackend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.experimental.UtilityClass;
import org.nodystudio.nodybackend.dto.code.ErrorCode;
import org.nodystudio.nodybackend.exception.custom.InvalidCoordinateException;

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
      throw new InvalidCoordinateException("위도와 경도는 null일 수 없습니다.", ErrorCode.INVALID_COORDINATE);
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
   * @throws InvalidCoordinateException 유효하지 않은 위도인 경우
   */
  public static void validateLatitude(BigDecimal latitude) {
    if (latitude == null) {
      throw new InvalidCoordinateException("위도는 null일 수 없습니다.", ErrorCode.INVALID_COORDINATE);
    }

    if (latitude.compareTo(BigDecimal.valueOf(-90.0)) < 0 ||
        latitude.compareTo(BigDecimal.valueOf(90.0)) > 0) {
      throw new InvalidCoordinateException("위도는 -90도에서 90도 사이여야 합니다. 입력값: " + latitude,
          ErrorCode.INVALID_COORDINATE);
    }
  }

  /**
   * 경도의 유효성을 검증합니다.
   *
   * @param longitude 검증할 경도 (-180 ~ 180)
   * @throws InvalidCoordinateException 유효하지 않은 경도인 경우
   */
  public static void validateLongitude(BigDecimal longitude) {
    if (longitude == null) {
      throw new InvalidCoordinateException("경도는 null일 수 없습니다.", ErrorCode.INVALID_COORDINATE);
    }

    if (longitude.compareTo(BigDecimal.valueOf(-180.0)) < 0 ||
        longitude.compareTo(BigDecimal.valueOf(180.0)) > 0) {
      throw new InvalidCoordinateException("경도는 -180도에서 180도 사이여야 합니다. 입력값: " + longitude,
          ErrorCode.INVALID_COORDINATE);
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

}
