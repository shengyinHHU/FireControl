package com.cn21.open.demo.common;

/**
 * @Author lyf15
 * @Description
 * @Date 2023/8/2917:16
 */
public class Api {
    /**
     * 获取设备的RTSP直播链接
     */
    public static final String GET_DEVICE_MEDIA_URL_RTSP_URI = "/open/token/cloud/getDeviceMediaUrlRtsp";
    /**
     * 获取设备HLS直播链接
     */
    public static final String GET_DEVICE_MEDIA_URL_HLS_URI = "/open/token/cloud/getDeviceMediaUrlHls";
    /**
     * 获取设备RTMP直播链接
     */
    public static final String GET_DEVICE_MEDIA_URL_RTMP_URI = "/open/token/cloud/getDeviceMediaUrlRtmp";
    /**
     * 查询云眼设备树目录结构
     */
    public static final String GET_REGIONS_URI = "/open/token/vcpTree/getRegions";
    /**
     * 获取设备树最后一层区域列表
     */
    public static final String GET_LAST_REGIONS_URI = "/open/token/vcpTree/getLastRegions";
    /**
     * 根据区域码分页查询设备树下设备列表
     */
    public static final String GET_DEVICES_BY_REGION_CODE_URI = "/open/token/vcpTree/getDevicesByRegionCode";
}
