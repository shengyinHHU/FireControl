package com.cn21.open.demo.common;

/**
 * @author lyf15
 * @date 2023/7/12
 * 公共参数-通用常量
 * 请将此次常量修改成合作方自身的相关参数，否则，网关校验，加解密测试等不通过
 **/
public class Constant {
    /**
     * 合作方申请的appId
     * */
    public static final String APP_ID = "08******39";
    /**
     * 合作方申请的appSecret
    */
    public static final String APP_SECRET = "3fe**************************9866";
    /**
     * 企业主账户对应手机号,只有平台应用需要填写修改，标准应用不需要用到此参数
     */
    public static final String ENTERPRISE_USER = "189*****776";
    /**
     * 服务端版本号，若公共参数version为v1.0，返回敏感参数使用XXTea加密
     */
    public static final String VERSION = "v1.0";
    /**
     * 服务端版本号，若公共参数version为（不加v前缀），返回敏感参数使用RSA加密
     */
    public static final String VERSION2 = "1.1";
    /**
     * 服务端版本号，如1.1，若公共参数version填写此值，返回敏感参数使用RSA加密（不加v前缀）
     */

    /**
     * 接入端类型 , 可选值：
     * 0-IOS
     * 1-Android
     * 2-Web/WAP/H5
     * 3-PC
     * 4-服务端
     */
    public static final Integer CLINT_TYPE = 3;
    /**
     * 公钥
     * 合作方自己生成的RSA加密秘钥中，用于本地解密的 本地私钥,这里不是真实的，请自己生成后填写这里。
     */
    public static final String PUBLIC_KEY  = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCuwIQwikxu/nCuqhgcCw/FLeuQn3usEibp7DG8WQNy4Mt7b4tMZdmAOU8sp5q77KAgZgqwsv0/hKuYLYxavBKhbvuQuEibk6HvuNbcBKIn1N4oAY6m49zNdRli4YxDovMMB6+wJexRTnPC3Bei1Py4DYAul0xlyxe5TewoqmFmhwIDAQAB";
    /**
     * 私钥
     */
    public static final String PRIVATE_KEY ="MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAK7AhDCKTG7+cK6qGBwLD8Ut65Cfe6wSJunsMbxZA3Lgy3tvi0xl2YA5TyynmrvsoCBmCrCy/T+Eq5gtjFq8EqFu+5C4SJuToe+41twEoifU3igBjqbj3M11GWLhjEOi8wwHr7Al7FFOc8LcF6LU/LgNgC6XTGXLF7lN7CiqYWaHAgMBAAECgYB76ckSPk66ebNfNv+HixRwpgIed+gS+IAdaT1PJ/M5F0tSSKMG1kWnFXwd1u7yiHR25MR5zUGkM6gVbV7Ty/6MygFMPu81CVPwlD846phaOPzOLNHFNXWCdH8LDW1B/4OwRhsT7Sz6sXo988A2BtdxX0KyPKYRZSOiTLF7Ei6mUQJBAO31AunimMf+ORE+RZoimidGjuBoR0kP3b/NXLNpcBYiQ6v9D0KWNbWUAe2S4Fdocq+FCDqgfXqmdSYeQRWeqikCQQC8AKFtZv9Fuz9mHSf+LVA5tWqdeoPQFr0G71A15tl+zYibGMRlfPIV87RFwsBp1ql4KJbcbiGkrtubgdF1pgEvAkEA3fsvwaHaZs84y2YtJfHbnoIK7Ts6LOU5NyFLti7JGkzSllIXd0WLSx2MguT+lWvheO2AVLi89brtFCcrDhEaqQJBAJIIA1wRdxPZIKPW3evHkttgmZH9Sknmf6nVVf6odCTUzdl7YYJbjUeT52GQpxkPDtAw5w4N1cAvupGOQv8ZlS0CQQCiSecU9HKGgG/oFdP6IU6aDhCwGUcIQpZR3FniKHcBp1C/YZAYo/2kbLik3wbWgZcBPx9vpGGeCHHdC8lTyhgJ";
    /**
     * 能力开放正式环境域名
     */
    public static final String PRO_DOMAIN = "https://vcp.21cn.com";
    /**
     * 能力开放测试环境域名，此环境不开放给合作方，请调用正式环境接入并测试
     */
    public static final String TEST_DOMAIN = "https://beta-vcp.21cn.com";
}
