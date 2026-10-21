package site.seaunit.zhishiku.auth.registration;

public final class RegistrationEmailTemplate {
  private RegistrationEmailTemplate() {}

  public static String text(String code, String brand) {
    return """
        你正在注册%s。

        邮箱验证码：%s

        验证码 10 分钟内有效，只能使用一次。
        这是一封系统邮件，请勿直接回复。如果你没有发起此操作，可以忽略它。
        """
        .formatted(brand, code);
  }

  public static String html(String code, String brand) {
    String safeBrand = escape(brand);
    String safeCode = escape(code);
    return """
        <!doctype html>
        <html lang="zh-CN">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <meta name="color-scheme" content="light" />
            <title>完成{{BRAND}}注册</title>
            <style>
              @media only screen and (max-width: 620px) {
                .email-shell { padding: 20px 12px !important; }
                .email-card { padding: 30px 22px !important; }
                .email-title { font-size: 25px !important; line-height: 32px !important; }
                .email-code { font-size: 34px !important; letter-spacing: 7px !important; }
                .email-brand-subtitle { display: none !important; }
              }
            </style>
          </head>
          <body style="margin:0;padding:0;background:#edf3f6;color:#132c38;">
            <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
              你的{{BRAND}}注册验证码是 {{CODE}}，10 分钟内有效。
            </div>
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"
              style="width:100%;background:#edf3f6;">
              <tr>
                <td class="email-shell" align="center" style="padding:42px 20px;">
                  <table role="presentation" width="640" cellspacing="0" cellpadding="0" border="0"
                    style="width:100%;max-width:640px;">
                    <tr>
                      <td style="padding:0 0 22px 4px;">
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                          <tr>
                            <td width="52" height="52" align="center" valign="middle"
                              style="width:52px;height:52px;border-radius:12px;background:#0b3144;
                              color:#ffffff;font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;
                              font-size:25px;font-weight:700;">识</td>
                            <td style="padding-left:14px;font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;">
                              <div style="font-size:23px;line-height:28px;font-weight:700;color:#102f3d;">
                                {{BRAND}}
                              </div>
                              <div class="email-brand-subtitle"
                                style="padding-top:2px;font-size:13px;line-height:18px;color:#70828a;">
                                个人知识库
                              </div>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="overflow:hidden;border:1px solid #d8e2e7;border-radius:14px;
                        background:#ffffff;">
                        <div style="height:4px;background:#0b3144;font-size:0;line-height:0;">&nbsp;</div>
                        <div class="email-card" style="padding:40px 46px 42px;">
                          <div style="font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;">
                            <h1 class="email-title"
                              style="margin:0;font-size:29px;line-height:38px;font-weight:700;color:#132c38;">
                              完成{{BRAND}}注册
                            </h1>
                            <p style="margin:14px 0 0;font-size:17px;line-height:29px;color:#52666f;">
                              请回到识海的「创建账户」页面，输入下面的 6 位验证码继续。
                            </p>
                          </div>
                          <div style="margin-top:30px;padding:26px 18px;border:1px solid #d8e2e7;
                            border-radius:10px;background:#f3f7f9;text-align:center;">
                            <div class="email-code"
                              style="font-family:'SFMono-Regular',Consolas,'Liberation Mono',monospace;
                              font-size:42px;line-height:50px;font-weight:700;letter-spacing:9px;
                              color:#102f3d;">{{CODE}}</div>
                          </div>
                          <p style="margin:24px 0 0;font-size:15px;line-height:24px;color:#647780;
                            font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;">
                            验证码 10 分钟内有效，且只能使用一次。
                          </p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 6px 0;font-family:'PingFang SC','Microsoft YaHei',Arial,sans-serif;
                        font-size:13px;line-height:22px;color:#71838b;">
                        这是一封系统邮件，请勿直接回复。如果你没有发起此操作，可以忽略它。
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """
        .replace("{{BRAND}}", safeBrand)
        .replace("{{CODE}}", safeCode);
  }

  private static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
