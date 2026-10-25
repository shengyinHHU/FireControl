package com.ruoyi.system.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.core.domain.BaseEntity;

/** 消防场所地图只读模型。 */
public class FirePlace extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long placeId;
    private String name;
    private String owner;
    private String phone;
    public String getOwner() { return owner; }
    public void setOwner(String value) { owner = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    private String placeType;
    private String industryCategory;
    private String managementCompany;
    private String address;
    private Long gridId;
    private String gridName;
    private String riskLevel;
    private String mainRiskSources;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String coordinateSystem;
    private String locationStatus;
    private String matchedAddress;
    private boolean mapOnly;

    @JsonSerialize(using = ToStringSerializer.class)
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlaceType() { return placeType; }
    public void setPlaceType(String placeType) { this.placeType = placeType; }

    public String getIndustryCategory() { return industryCategory; }
    public void setIndustryCategory(String industryCategory) { this.industryCategory = industryCategory; }

    public String getManagementCompany() { return managementCompany; }
    public void setManagementCompany(String managementCompany) { this.managementCompany = managementCompany; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @JsonSerialize(using = ToStringSerializer.class)
    public Long getGridId() { return gridId; }
    public void setGridId(Long gridId) { this.gridId = gridId; }

    public String getGridName() { return gridName; }
    public void setGridName(String gridName) { this.gridName = gridName; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getMainRiskSources() { return mainRiskSources; }
    public void setMainRiskSources(String mainRiskSources) { this.mainRiskSources = mainRiskSources; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public String getCoordinateSystem() { return coordinateSystem; }
    public void setCoordinateSystem(String coordinateSystem) { this.coordinateSystem = coordinateSystem; }

    public String getLocationStatus() { return locationStatus; }
    public void setLocationStatus(String locationStatus) { this.locationStatus = locationStatus; }

    public String getMatchedAddress() { return matchedAddress; }
    public void setMatchedAddress(String matchedAddress) { this.matchedAddress = matchedAddress; }
    public boolean isMapOnly() { return mapOnly; }
    public void setMapOnly(boolean mapOnly) { this.mapOnly = mapOnly; }
}

