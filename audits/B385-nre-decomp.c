/* rsdd ghidra C export
   program : nre.dll
   filter  : encrypt0|decrypt0|isPasswordValid0|getSystemPassword0|addUserAccount0
*/

/* ---- Java_com_tridium_nre_platform_NativePlatformProvider_addUserAccount0 @ 180002520 ---- */

undefined8 Java_com_tridium_nre_platform_NativePlatformProvider_addUserAccount0(void)

{
                    /* 0x2520  121
                       Java_com_tridium_nre_platform_NativePlatformProvider_addUserAccount0
                       0x2520  132
                       Java_com_tridium_nre_platform_NativePlatformProvider_executeNativeDiagnosticsCommand0
                       0x2520  171
                       Java_com_tridium_nre_platform_NativePlatformProvider_getNativeDiagnosticsCommands0
                       0x2520  182
                       Java_com_tridium_nre_platform_NativePlatformProvider_getSystemPassword0 */
  return 0;
}



/* ---- Java_com_tridium_nre_platform_NativePlatformProvider_isPasswordValid0 @ 180004960 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */

void Java_com_tridium_nre_platform_NativePlatformProvider_isPasswordValid0
               (JNIEnv_ *param_1,undefined8 param_2,_jstring *param_3,_jstring *param_4)

{
  int iVar1;
  undefined1 auStack_98 [32];
  char *local_78;
  undefined8 uStack_70;
  undefined8 local_68;
  undefined8 uStack_60;
  undefined8 local_58;
  char *local_50;
  undefined8 uStack_48;
  undefined8 local_40;
  undefined8 uStack_38;
  undefined8 local_30;
  ulonglong local_28;
  
                    /* 0x4960  195
                       Java_com_tridium_nre_platform_NativePlatformProvider_isPasswordValid0 */
  local_28 = DAT_18001a1c8 ^ (ulonglong)auStack_98;
  if ((((param_3 != (_jstring *)0x0) && (param_4 != (_jstring *)0x0)) &&
      (iVar1 = (**(code **)(*(longlong *)param_1 + 0x540))(param_1,param_3), iVar1 < 0x1001)) &&
     (iVar1 = (**(code **)(*(longlong *)param_1 + 0x540))(param_1,param_4), iVar1 < 0x1001)) {
    local_30 = 0;
    local_50 = (char *)0x0;
    uStack_48 = 0;
    local_40 = 0;
    uStack_38 = 0;
    JNIString::JNIString((JNIString *)&local_50,param_1,param_3);
    local_58 = 0;
    local_78 = (char *)0x0;
    uStack_70 = 0;
    local_68 = 0;
    uStack_60 = 0;
    JNIString::JNIString((JNIString *)&local_78,param_1,param_4);
    AuthenticationUtil::isPasswordValid(local_50,local_78);
    JNIString::~JNIString((JNIString *)&local_78);
    JNIString::~JNIString((JNIString *)&local_50);
  }
  FUN_18000ce30(local_28 ^ (ulonglong)auStack_98);
  return;
}



/* ---- Java_com_tridium_nre_util_DpapiUtil_decrypt0 @ 18000b740 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */

void Java_com_tridium_nre_util_DpapiUtil_decrypt0
               (longlong *param_1,undefined8 param_2,longlong param_3,char param_4)

{
  DWORD DVar1;
  int iVar2;
  undefined8 uVar3;
  longlong lVar4;
  int iVar5;
  _CRYPTOAPI_BLOB *p_Var6;
  char *pcVar7;
  undefined1 auStack_488 [32];
  BYTE *local_468;
  _CRYPTOAPI_BLOB local_458;
  _CRYPTOAPI_BLOB local_448;
  undefined1 local_438 [1024];
  ulonglong local_38;
  
                    /* 0xb740  223  Java_com_tridium_nre_util_DpapiUtil_decrypt0 */
  local_38 = DAT_18001a1c8 ^ (ulonglong)auStack_488;
  iVar5 = 0;
  local_448.cbData = 0;
  local_448._4_4_ = 0;
  local_448.pbData = (BYTE *)0x0;
  local_458._0_8_ = 0;
  local_458.pbData = (BYTE *)0x0;
  memset(local_438,0,0x400);
  if (param_3 == 0) {
    pcVar7 = "Null encrypted value";
  }
  else {
    DVar1 = (**(code **)(*param_1 + 0x558))(param_1,param_3);
    if ((int)DVar1 < 0x1001) {
      local_448.pbData = (BYTE *)(**(code **)(*param_1 + 0x5c0))(param_1,param_3,0);
      local_448.cbData = DVar1;
      iVar2 = DpapiHelper::decrypt(&local_448,&local_458,param_4 == '\x01');
      if (iVar2 == 0) {
        pcVar7 = "Error decrypting value";
      }
      else {
        lVar4 = (**(code **)(*param_1 + 0x580))(param_1,local_458._0_8_ & 0xffffffff);
        if (lVar4 == 0) {
          pcVar7 = "Error allocating Java byte array";
        }
        else {
          local_468 = local_458.pbData;
          (**(code **)(*param_1 + 0x680))(param_1,lVar4,0,local_458._0_8_ & 0xffffffff);
          lVar4 = (**(code **)(*param_1 + 0x78))(param_1);
          if (lVar4 == 0) goto LAB_18000b8a4;
          pcVar7 = "Error copying the data into the Java byte array";
        }
      }
    }
    else {
      pcVar7 = "Too large encrypted value";
    }
  }
  iVar5 = -1;
  FUN_1800052d0(local_438,0x400,0xffffffffffffffff,pcVar7);
LAB_18000b8a4:
  if (local_448.pbData != (BYTE *)0x0) {
    p_Var6 = &local_448;
    for (lVar4 = 0x10; lVar4 != 0; lVar4 = lVar4 + -1) {
      *(undefined1 *)&p_Var6->cbData = 0;
      p_Var6 = (_CRYPTOAPI_BLOB *)((longlong)&p_Var6->cbData + 1);
    }
    LocalFree(local_448.pbData);
  }
  if (local_458.pbData != (BYTE *)0x0) {
    p_Var6 = &local_458;
    for (lVar4 = 0x10; lVar4 != 0; lVar4 = lVar4 + -1) {
      *(undefined1 *)&p_Var6->cbData = 0;
      p_Var6 = (_CRYPTOAPI_BLOB *)((longlong)&p_Var6->cbData + 1);
    }
    LocalFree(local_458.pbData);
  }
  if (iVar5 != 0) {
    uVar3 = (**(code **)(*param_1 + 0x30))(param_1,"java/lang/SecurityException");
    (**(code **)(*param_1 + 0x70))(param_1,uVar3,local_438);
  }
  FUN_18000ce30(local_38 ^ (ulonglong)auStack_488);
  return;
}



/* ---- Java_com_tridium_nre_util_DpapiUtil_encrypt0 @ 18000b960 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */

void Java_com_tridium_nre_util_DpapiUtil_encrypt0
               (longlong *param_1,undefined8 param_2,longlong param_3,char param_4,char param_5)

{
  DWORD DVar1;
  int iVar2;
  undefined8 uVar3;
  longlong lVar4;
  int iVar5;
  _CRYPTOAPI_BLOB *p_Var6;
  char *pcVar7;
  undefined1 auStack_498 [32];
  BYTE *local_478;
  _CRYPTOAPI_BLOB local_468;
  _CRYPTOAPI_BLOB local_458;
  undefined1 local_448 [1024];
  ulonglong local_48;
  
                    /* 0xb960  224  Java_com_tridium_nre_util_DpapiUtil_encrypt0 */
  local_48 = DAT_18001a1c8 ^ (ulonglong)auStack_498;
  iVar5 = 0;
  local_458.cbData = 0;
  local_458._4_4_ = 0;
  local_458.pbData = (BYTE *)0x0;
  local_468._0_8_ = 0;
  local_468.pbData = (BYTE *)0x0;
  memset(local_448,0,0x400);
  if (param_3 == 0) {
    pcVar7 = "Null data value";
  }
  else {
    DVar1 = (**(code **)(*param_1 + 0x558))(param_1,param_3);
    if ((int)DVar1 < 0x1001) {
      local_458.pbData = (BYTE *)(**(code **)(*param_1 + 0x5c0))(param_1,param_3,0);
      local_458.cbData = DVar1;
      iVar2 = DpapiHelper::encrypt(&local_458,&local_468,param_4 == '\x01',param_5 == '\x01');
      if (iVar2 == 0) {
        pcVar7 = "Error encrypting value";
      }
      else {
        lVar4 = (**(code **)(*param_1 + 0x580))(param_1,local_468._0_8_ & 0xffffffff);
        if (lVar4 == 0) {
          pcVar7 = "Error allocating Java byte array";
        }
        else {
          local_478 = local_468.pbData;
          (**(code **)(*param_1 + 0x680))(param_1,lVar4,0,local_468._0_8_ & 0xffffffff);
          lVar4 = (**(code **)(*param_1 + 0x78))(param_1);
          if (lVar4 == 0) goto LAB_18000bad8;
          pcVar7 = "Error copying the data into the Java byte array";
        }
      }
    }
    else {
      pcVar7 = "Too large data value";
    }
  }
  iVar5 = -1;
  FUN_1800052d0(local_448,0x400,0xffffffffffffffff,pcVar7);
LAB_18000bad8:
  if (local_458.pbData != (BYTE *)0x0) {
    p_Var6 = &local_458;
    for (lVar4 = 0x10; lVar4 != 0; lVar4 = lVar4 + -1) {
      *(undefined1 *)&p_Var6->cbData = 0;
      p_Var6 = (_CRYPTOAPI_BLOB *)((longlong)&p_Var6->cbData + 1);
    }
    LocalFree(local_458.pbData);
  }
  if (local_468.pbData != (BYTE *)0x0) {
    p_Var6 = &local_468;
    for (lVar4 = 0x10; lVar4 != 0; lVar4 = lVar4 + -1) {
      *(undefined1 *)&p_Var6->cbData = 0;
      p_Var6 = (_CRYPTOAPI_BLOB *)((longlong)&p_Var6->cbData + 1);
    }
    LocalFree(local_468.pbData);
  }
  if (iVar5 != 0) {
    uVar3 = (**(code **)(*param_1 + 0x30))(param_1,"java/lang/SecurityException");
    (**(code **)(*param_1 + 0x70))(param_1,uVar3,local_448);
  }
  FUN_18000ce30(local_48 ^ (ulonglong)auStack_498);
  return;
}


