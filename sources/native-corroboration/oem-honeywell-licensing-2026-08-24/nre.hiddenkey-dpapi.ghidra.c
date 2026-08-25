/* rsdd ghidra C export
   program : nre.dll
   filter  : HiddenKey|[Dd]papi|EncryptedRegistry
*/

/* ---- getOrCreateHiddenKey @ 180009f40 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* private: void __cdecl NreWin32::getOrCreateHiddenKey(char * __ptr64,int) __ptr64 */

void __thiscall NreWin32::getOrCreateHiddenKey(NreWin32 *this,char *param_1,int param_2)

{
  int iVar1;
  undefined8 uVar2;
  longlong *plVar3;
  longlong lVar4;
  undefined8 uVar5;
  longlong lVar6;
  longlong lVar7;
  undefined4 in_register_00000084;
  undefined1 *puVar8;
  ulonglong uVar9;
  undefined8 in_R9;
  
                    /* 0x9f40  74  ?getOrCreateHiddenKey@NreWin32@@AEAAXPEADH@Z */
  uVar5 = CONCAT44(in_register_00000084,param_2);
  uVar9 = (ulonglong)(uint)param_2;
  iVar1 = migrateHiddenKey(this);
  if (iVar1 != 0) {
    uVar2 = __acrt_iob_func(2);
    FUN_180008790(uVar2,"ERROR: Can not migrate lk.\n",uVar5,in_R9);
                    /* WARNING: Subroutine does not return */
    exit(0xf9);
  }
  iVar1 = readHiddenKey(this,param_1,param_2);
  if (iVar1 != 0) {
    plVar3 = FUN_180001470();
    if (plVar3 != (longlong *)0x0) {
      lVar4 = (**(code **)*plVar3)(plVar3,"disableHostIdGeneration","false");
      puVar8 = &DAT_18000fd10;
      lVar6 = 0;
      while( true ) {
        lVar7 = lVar6 + 1;
        if (*(char *)(lVar4 + lVar6) != (&DAT_18000fd10)[lVar6]) break;
        lVar6 = lVar7;
        if (lVar7 == 5) {
          uVar5 = __acrt_iob_func(2);
          FUN_180008790(uVar5,"ERROR: Host Id cannot be found/generated.\n",puVar8,plVar3);
                    /* WARNING: Subroutine does not return */
          exit(0xf9);
        }
      }
    }
    iVar1 = generateNewKey(this,param_1,param_2);
    if (iVar1 != 0) {
      uVar5 = __acrt_iob_func(2);
      FUN_180008790(uVar5,"ERROR: Can not generate lk.\n",uVar9,plVar3);
                    /* WARNING: Subroutine does not return */
      exit(0xf9);
    }
    iVar1 = saveHiddenKey(this,param_1);
    if (iVar1 != 0) {
      uVar5 = __acrt_iob_func(2);
      FUN_180008790(uVar5,"ERROR: Can not save lk.\n",uVar9,plVar3);
                    /* WARNING: Subroutine does not return */
      exit(0xf9);
    }
  }
  return;
}



/* ---- migrateHiddenKey @ 18000a740 ---- */

/* private: int __cdecl NreWin32::migrateHiddenKey(void) __ptr64 */

int __thiscall NreWin32::migrateHiddenKey(NreWin32 *this)

{
  LSTATUS LVar1;
  ulong uVar2;
  BOOL BVar3;
  int iVar4;
  undefined8 uVar5;
  undefined8 uVar6;
  undefined8 uVar7;
  DATA_BLOB *pOptionalEntropy;
  undefined1 auStackY_1b8 [32];
  HKEY local_168;
  DATA_BLOB local_160;
  DATA_BLOB local_150;
  DWORD local_140 [2];
  DATA_BLOB local_138;
  DWORD local_128 [4];
  BYTE local_118 [256];
  ulonglong local_18;
  
                    /* 0xa740  100  ?migrateHiddenKey@NreWin32@@AEAAHXZ */
  local_18 = DAT_18001a1c8 ^ (ulonglong)auStackY_1b8;
  LVar1 = RegOpenKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Niagara4_18001a130,0,1,&local_168);
  if (LVar1 == 0) {
    LVar1 = RegQueryValueExA(local_168,PTR_DAT_18001a138,(LPDWORD)0x0,(LPDWORD)0x0,(LPBYTE)0x0,
                             (LPDWORD)0x0);
    if (LVar1 == 0) {
      RegCloseKey(local_168);
      goto LAB_18000aa9b;
    }
    RegCloseKey(local_168);
  }
  LVar1 = RegOpenKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Classes_AppID_18001a128,0,1,
                        &local_168);
  if (LVar1 != 0) goto LAB_18000aa9b;
  memset(local_118,0,0xff);
  uVar7 = 0;
  uVar6 = 0;
  local_140[0] = 0xff;
  uVar2 = RegQueryValueExA(local_168,PTR_DAT_18001a138,(LPDWORD)0x0,(LPDWORD)0x0,local_118,local_140
                          );
  if (uVar2 == 2) {
LAB_18000aa86:
    RegCloseKey(local_168);
  }
  else {
    if (uVar2 == 0) {
      if (DAT_18001a4b4 != 0) {
        uVar6 = __acrt_iob_func(2);
        FUN_180008790(uVar6,"MESSAGE NreWin32.MHK: Start %s\n",local_118,uVar7);
      }
      RegCloseKey(local_168);
      uVar7 = 0;
      uVar6 = 0;
      uVar2 = RegCreateKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Niagara4_18001a130,0,
                              (LPSTR)0x0,0,0xf003f,(LPSECURITY_ATTRIBUTES)0x0,&local_168,local_128);
      if (uVar2 != 0) {
        if (DAT_18001a4b4 != 0) {
          uVar5 = __acrt_iob_func(2);
          FUN_180008790(uVar5,"ERROR NreWin32.MHK.RCKE: ",uVar6,uVar7);
          printError(this,uVar2);
        }
        goto LAB_18000aa9b;
      }
      uVar6 = 0;
      pOptionalEntropy = &local_138;
      local_138.cbData = DAT_18001a170;
      local_150.pbData = local_118;
      local_150.cbData = local_140[0];
      local_138.pbData = &DAT_18001a150;
      BVar3 = CryptProtectData(&local_150,(LPCWSTR)0x0,pOptionalEntropy,(PVOID)0x0,
                               (CRYPTPROTECT_PROMPTSTRUCT *)0x0,0x14,&local_160);
      if (BVar3 != 0) {
        uVar7 = 3;
        uVar6 = 0;
        uVar2 = RegSetValueExA(local_168,PTR_DAT_18001a138,0,3,local_160.pbData,local_160.cbData);
        if (uVar2 != 0) {
          if (DAT_18001a4b4 != 0) {
            uVar5 = __acrt_iob_func(2);
            FUN_180008790(uVar5,"ERROR NreWin32.MHK.RSVE: ",uVar6,uVar7);
            printError(this,uVar2);
          }
          LocalFree(local_160.pbData);
          RegCloseKey(local_168);
          goto LAB_18000aa9b;
        }
        if (DAT_18001a4b4 != 0) {
          uVar5 = __acrt_iob_func(2);
          FUN_180008790(uVar5,"MESSAGE NreWin32.MHK: Complete\n",uVar6,uVar7);
        }
        LocalFree(local_160.pbData);
        RegFlushKey(local_168);
        goto LAB_18000aa86;
      }
      if (DAT_18001a4b4 != 0) {
        uVar7 = __acrt_iob_func(2);
        FUN_180008790(uVar7,"ERROR NreWin32.MHK.CPD: ",pOptionalEntropy,uVar6);
        uVar2 = 0;
        goto LAB_18000a9b5;
      }
    }
    else if (DAT_18001a4b4 != 0) {
      uVar5 = __acrt_iob_func(2);
      FUN_180008790(uVar5,"ERROR NreWin32.MHK.RQVE: ",uVar6,uVar7);
LAB_18000a9b5:
      printError(this,uVar2);
    }
    RegCloseKey(local_168);
  }
LAB_18000aa9b:
  iVar4 = FUN_18000ce30(local_18 ^ (ulonglong)auStackY_1b8);
  return iVar4;
}



/* ---- readHiddenKey @ 18000acb0 ---- */

/* private: int __cdecl NreWin32::readHiddenKey(char * __ptr64,int) __ptr64 */

int __thiscall NreWin32::readHiddenKey(NreWin32 *this,char *param_1,int param_2)

{
  ulong uVar1;
  BOOL BVar2;
  int iVar3;
  undefined8 uVar4;
  int *piVar5;
  ulonglong _Size;
  ulonglong _Size_00;
  undefined8 uVar6;
  DATA_BLOB *pOptionalEntropy;
  undefined8 uVar7;
  undefined1 auStackY_a8 [32];
  HKEY local_68;
  DATA_BLOB local_60;
  DATA_BLOB local_50;
  DATA_BLOB local_38;
  ulonglong local_28;
  
                    /* 0xacb0  111  ?readHiddenKey@NreWin32@@AEAAHPEADH@Z */
  local_28 = DAT_18001a1c8 ^ (ulonglong)auStackY_a8;
  _Size_00 = (ulonglong)param_2;
  uVar6 = 0;
  uVar7 = 1;
  uVar1 = RegOpenKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Niagara4_18001a130,0,1,&local_68);
  if (uVar1 != 0) {
    if (DAT_18001a4b4 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_180008790(uVar4,"ERROR NreWin32.RHK.ROKE: ",uVar6,uVar7);
      printError(this,uVar1);
    }
    goto LAB_18000afbf;
  }
  uVar7 = 0;
  uVar6 = 0;
  uVar1 = RegQueryValueExA(local_68,PTR_DAT_18001a138,(LPDWORD)0x0,(LPDWORD)0x0,(LPBYTE)0x0,
                           &local_60.cbData);
  if (uVar1 != 0) {
    if (DAT_18001a4b4 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_180008790(uVar4,"ERROR NreWin32.RHK.RQVE: ",uVar6,uVar7);
      printError(this,uVar1);
    }
    RegCloseKey(local_68);
    goto LAB_18000afbf;
  }
  if (local_60.cbData == 0) {
    if (DAT_18001a4b4 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_180008790(uVar4,"ERROR NreWin32.RHK.RQVE.DATA = 0\n",uVar6,uVar7);
    }
    RegCloseKey(local_68);
    goto LAB_18000afbf;
  }
  local_60.pbData = LocalAlloc(0x40,(ulonglong)local_60.cbData);
  if (local_60.pbData == (LPBYTE)0x0) {
    if (DAT_18001a4b4 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_180008790(uVar4,"ERROR NreWin32.RHK.RQVE.LA = 0\n",uVar6,uVar7);
    }
    RegCloseKey(local_68);
    goto LAB_18000afbf;
  }
  uVar7 = 0;
  uVar6 = 0;
  uVar1 = RegQueryValueExA(local_68,PTR_DAT_18001a138,(LPDWORD)0x0,(LPDWORD)0x0,local_60.pbData,
                           &local_60.cbData);
  if (uVar1 == 0) {
    RegCloseKey(local_68);
    uVar6 = 0;
    pOptionalEntropy = &local_38;
    local_38.cbData = DAT_18001a170;
    local_38.pbData = &DAT_18001a150;
    BVar2 = CryptUnprotectData(&local_60,(LPWSTR *)0x0,pOptionalEntropy,(PVOID)0x0,
                               (CRYPTPROTECT_PROMPTSTRUCT *)0x0,0,&local_50);
    if (BVar2 != 0) {
      _Size = (ulonglong)local_50.cbData;
      if (_Size != 0) {
        if (param_1 == (char *)0x0) {
LAB_18000af51:
          piVar5 = _errno();
          *piVar5 = 0x16;
        }
        else {
          if ((local_50.pbData != (BYTE *)0x0) && (_Size <= _Size_00)) {
            memcpy(param_1,local_50.pbData,_Size);
            goto LAB_18000afa7;
          }
          memset(param_1,0,_Size_00);
          if (local_50.pbData == (BYTE *)0x0) goto LAB_18000af51;
          if (_Size <= _Size_00) goto LAB_18000afa7;
          piVar5 = _errno();
          *piVar5 = 0x22;
        }
        _invalid_parameter_noinfo();
      }
LAB_18000afa7:
      LocalFree(local_50.pbData);
      LocalFree(local_60.pbData);
      goto LAB_18000afbf;
    }
    if (DAT_18001a4b4 != 0) {
      uVar7 = __acrt_iob_func(2);
      FUN_180008790(uVar7,"ERROR NreWin32.RHK.CUD: ",pOptionalEntropy,uVar6);
      printError(this,0);
    }
  }
  else {
    if (DAT_18001a4b4 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_180008790(uVar4,"ERROR NreWin32.RHK.RQVE.2: ",uVar6,uVar7);
      printError(this,uVar1);
    }
    RegCloseKey(local_68);
  }
  LocalFree(local_60.pbData);
LAB_18000afbf:
  iVar3 = FUN_18000ce30(local_28 ^ (ulonglong)auStackY_a8);
  return iVar3;
}



/* ---- saveHiddenKey @ 18000b140 ---- */

/* private: int __cdecl NreWin32::saveHiddenKey(char * __ptr64) __ptr64 */

int __thiscall NreWin32::saveHiddenKey(NreWin32 *this,char *param_1)

{
  ulong uVar1;
  BOOL BVar2;
  int iVar3;
  undefined8 uVar4;
  longlong lVar5;
  undefined8 uVar6;
  undefined8 uVar7;
  DATA_BLOB *pOptionalEntropy;
  undefined1 auStackY_a8 [32];
  HKEY local_58;
  DATA_BLOB local_50;
  DATA_BLOB local_40;
  DATA_BLOB local_28;
  DWORD local_18 [2];
  ulonglong local_10;
  
                    /* 0xb140  113  ?saveHiddenKey@NreWin32@@AEAAHPEAD@Z */
  local_10 = DAT_18001a1c8 ^ (ulonglong)auStackY_a8;
  uVar6 = 0;
  uVar7 = 0;
  uVar1 = RegCreateKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Niagara4_18001a130,0,(LPSTR)0x0,0,
                          0xf003f,(LPSECURITY_ATTRIBUTES)0x0,&local_58,local_18);
  if (uVar1 == 0) {
    local_28.cbData = DAT_18001a170;
    lVar5 = -1;
    local_28.pbData = &DAT_18001a150;
    do {
      lVar5 = lVar5 + 1;
    } while (param_1[lVar5] != '\0');
    local_40.cbData = (DWORD)lVar5;
    pOptionalEntropy = &local_28;
    uVar7 = 0;
    local_40.pbData = (BYTE *)param_1;
    BVar2 = CryptProtectData(&local_40,(LPCWSTR)0x0,pOptionalEntropy,(PVOID)0x0,
                             (CRYPTPROTECT_PROMPTSTRUCT *)0x0,0x14,&local_50);
    if (BVar2 == 0) {
      if (DAT_18001a4b4 != 0) {
        uVar6 = __acrt_iob_func(2);
        FUN_180008790(uVar6,"ERROR NreWin32.SHK.CPD: ",pOptionalEntropy,uVar7);
        printError(this,0);
      }
      RegCloseKey(local_58);
    }
    else {
      uVar6 = 3;
      uVar7 = 0;
      uVar1 = RegSetValueExA(local_58,PTR_DAT_18001a138,0,3,local_50.pbData,local_50.cbData);
      if (uVar1 == 0) {
        LocalFree(local_50.pbData);
        RegFlushKey(local_58);
        RegCloseKey(local_58);
      }
      else {
        if (DAT_18001a4b4 != 0) {
          uVar4 = __acrt_iob_func(2);
          FUN_180008790(uVar4,"ERROR NreWin32.SHK.RSVE: ",uVar7,uVar6);
          printError(this,uVar1);
        }
        LocalFree(local_50.pbData);
        RegCloseKey(local_58);
      }
    }
  }
  else if (DAT_18001a4b4 != 0) {
    uVar4 = __acrt_iob_func(2);
    FUN_180008790(uVar4,"ERROR NreWin32.SHK.RCKE: ",uVar7,uVar6);
    printError(this,uVar1);
  }
  iVar3 = FUN_18000ce30(local_10 ^ (ulonglong)auStackY_a8);
  return iVar3;
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



/* ---- Java_com_tridium_nre_util_RegistryUtil_getEncryptedRegistryString0 @ 18000bde0 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */

void Java_com_tridium_nre_util_RegistryUtil_getEncryptedRegistryString0
               (longlong *param_1,undefined8 param_2,longlong param_3,char param_4,char param_5)

{
  bool bVar1;
  bool bVar2;
  int iVar3;
  LSTATUS LVar4;
  int iVar5;
  LPCSTR lpValueName;
  longlong lVar6;
  undefined8 uVar7;
  longlong lVar8;
  DATA_BLOB *pDVar9;
  char *pcVar10;
  undefined1 auStackY_4d8 [32];
  HKEY local_478;
  DATA_BLOB local_470;
  DATA_BLOB local_460;
  DWORD local_450 [2];
  undefined1 local_448 [1024];
  ulonglong local_48;
  
                    /* 0xbde0  225
                       Java_com_tridium_nre_util_RegistryUtil_getEncryptedRegistryString0 */
  local_48 = DAT_18001a1c8 ^ (ulonglong)auStackY_4d8;
  lVar6 = 0;
  local_478 = (HKEY)0x0;
  local_450[0] = 0;
  bVar2 = false;
  if ((param_3 == 0) || (iVar3 = (**(code **)(*param_1 + 0x540))(param_1,param_3), 0x1000 < iVar3))
  goto LAB_18000c209;
  local_470._0_8_ = 0;
  local_470.pbData = (LPBYTE)0x0;
  local_460._0_8_ = 0;
  local_460.pbData = (BYTE *)0x0;
  lpValueName = (LPCSTR)(**(code **)(*param_1 + 0x548))(param_1,param_3,0);
  iVar3 = -1;
  memset(local_448,0,0x400);
  LVar4 = RegCreateKeyExA((HKEY)0xffffffff80000002,"SOFTWARE\\Niagara4",0,(LPSTR)0x0,0,0x20119,
                          (LPSECURITY_ATTRIBUTES)0x0,&local_478,local_450);
  if (LVar4 == 0) {
    LVar4 = RegQueryValueExA(local_478,lpValueName,(LPDWORD)0x0,(LPDWORD)0x0,(LPBYTE)0x0,
                             &local_470.cbData);
    if (LVar4 == 0) {
      bVar1 = false;
LAB_18000c01a:
      if (local_470.cbData == 0) {
LAB_18000c14d:
        iVar3 = 0;
        goto LAB_18000c14f;
      }
      local_470.pbData = LocalAlloc(0x40,local_470._0_8_ & 0xffffffff);
      if (local_470.pbData == (LPBYTE)0x0) {
        pcVar10 = "Error allocating space for the registry value (error = %ld)\n";
      }
      else {
        LVar4 = RegQueryValueExA(local_478,lpValueName,(LPDWORD)0x0,(LPDWORD)0x0,local_470.pbData,
                                 &local_470.cbData);
        if (LVar4 == 0) {
          if (bVar1) {
            iVar5 = CryptUnprotectData(&local_470,(LPWSTR *)0x0,(DATA_BLOB *)0x0,(PVOID)0x0,
                                       (CRYPTPROTECT_PROMPTSTRUCT *)0x0,0,&local_460);
          }
          else {
            iVar5 = DpapiHelper::decrypt(&local_470,&local_460,param_5 == '\x01');
          }
          if (iVar5 == 0) {
            pcVar10 = "Error decrypting registry value\n";
          }
          else {
            lVar6 = (**(code **)(*param_1 + 0x580))(param_1,local_460._0_8_ & 0xffffffff);
            if (lVar6 == 0) {
              pcVar10 = "Error allocating Java byte array\n";
            }
            else {
              (**(code **)(*param_1 + 0x680))(param_1,lVar6,0,local_460._0_8_ & 0xffffffff);
              lVar8 = (**(code **)(*param_1 + 0x78))(param_1);
              if (lVar8 == 0) goto LAB_18000c14d;
              pcVar10 = "Error copying the data into the Java byte array\n";
            }
          }
          FUN_1800052d0(local_448,0x400,0xffffffffffffffff,pcVar10);
          goto LAB_18000c14f;
        }
        pcVar10 = "Error reading registry key data (error = %ld)\n";
      }
    }
    else {
      if (LVar4 == 2) {
        if (param_4 == '\x01') {
          if (local_478 != (HKEY)0x0) {
            RegCloseKey(local_478);
            local_478 = (HKEY)0x0;
          }
          LVar4 = RegCreateKeyExA((HKEY)0xffffffff80000002,"SOFTWARE\\Niagara",0,(LPSTR)0x0,0,
                                  0x20119,(LPSECURITY_ATTRIBUTES)0x0,&local_478,local_450);
          if (LVar4 != 0) goto LAB_18000bef1;
          LVar4 = RegQueryValueExA(local_478,lpValueName,(LPDWORD)0x0,(LPDWORD)0x0,(LPBYTE)0x0,
                                   &local_470.cbData);
          if (LVar4 != 2) {
            if (LVar4 == 0) {
              bVar1 = true;
              bVar2 = true;
              goto LAB_18000c01a;
            }
            goto LAB_18000bf49;
          }
        }
        goto LAB_18000c14d;
      }
LAB_18000bf49:
      pcVar10 = "Error opening registry key (error = %ld)\n";
    }
    FUN_1800052d0(local_448,0x400,0xffffffffffffffff,pcVar10);
    lVar6 = 0;
  }
  else {
LAB_18000bef1:
    FUN_1800052d0(local_448,0x400,0xffffffffffffffff,"Error creating registry key (error = %ld)\n");
    local_478 = (HKEY)0x0;
  }
LAB_18000c14f:
  if (lpValueName != (LPCSTR)0x0) {
    (**(code **)(*param_1 + 0x550))(param_1,param_3,lpValueName);
  }
  if (local_478 != (HKEY)0x0) {
    RegCloseKey(local_478);
    local_478 = (HKEY)0x0;
  }
  if (local_470.pbData != (LPBYTE)0x0) {
    pDVar9 = &local_470;
    for (lVar8 = 0x10; lVar8 != 0; lVar8 = lVar8 + -1) {
      *(undefined1 *)&pDVar9->cbData = 0;
      pDVar9 = (DATA_BLOB *)((longlong)&pDVar9->cbData + 1);
    }
    LocalFree(local_470.pbData);
  }
  if (local_460.pbData != (BYTE *)0x0) {
    pDVar9 = &local_460;
    for (lVar8 = 0x10; lVar8 != 0; lVar8 = lVar8 + -1) {
      *(undefined1 *)&pDVar9->cbData = 0;
      pDVar9 = (DATA_BLOB *)((longlong)&pDVar9->cbData + 1);
    }
    LocalFree(local_460.pbData);
  }
  if (iVar3 == 0) {
    if (bVar2) {
      Java_com_tridium_nre_util_RegistryUtil_setEncryptedRegistryString0
                (param_1,param_2,param_3,lVar6,param_5);
    }
  }
  else {
    uVar7 = (**(code **)(*param_1 + 0x30))(param_1,"java/lang/SecurityException");
    (**(code **)(*param_1 + 0x70))(param_1,uVar7,local_448);
  }
LAB_18000c209:
  FUN_18000ce30(local_48 ^ (ulonglong)auStackY_4d8);
  return;
}



/* ---- Java_com_tridium_nre_util_RegistryUtil_setEncryptedRegistryString0 @ 18000c260 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */

void Java_com_tridium_nre_util_RegistryUtil_setEncryptedRegistryString0
               (longlong *param_1,undefined8 param_2,longlong param_3,longlong param_4,char param_5)

{
  LSTATUS LVar1;
  DWORD DVar2;
  int iVar3;
  LPCSTR lpValueName;
  undefined8 uVar4;
  BYTE *pBVar5;
  longlong lVar6;
  BYTE *pBVar7;
  BYTE *pBVar8;
  _CRYPTOAPI_BLOB *p_Var9;
  BYTE *pBVar10;
  undefined1 auStackY_4c8 [32];
  HKEY local_478;
  _CRYPTOAPI_BLOB local_470;
  _CRYPTOAPI_BLOB local_460;
  DWORD local_450 [2];
  undefined1 local_448 [1024];
  ulonglong local_48;
  
                    /* 0xc260  226
                       Java_com_tridium_nre_util_RegistryUtil_setEncryptedRegistryString0 */
  local_48 = DAT_18001a1c8 ^ (ulonglong)auStackY_4c8;
  pBVar10 = (BYTE *)0x0;
  local_478 = (HKEY)0x0;
  local_450[0] = 0;
  if (((param_3 != 0) && (iVar3 = (**(code **)(*param_1 + 0x540))(param_1,param_3), iVar3 < 0x1001))
     && ((param_4 == 0 || (iVar3 = (**(code **)(*param_1 + 0x558))(param_1,param_4), iVar3 < 0x1001)
         ))) {
    local_460.cbData = 0;
    local_460._4_4_ = 0;
    local_460.pbData = (BYTE *)0x0;
    local_470.cbData = 0;
    local_470._4_4_ = 0;
    local_470.pbData = (BYTE *)0x0;
    lpValueName = (LPCSTR)(**(code **)(*param_1 + 0x548))(param_1,param_3,0);
    pBVar7 = (BYTE *)0xffffffffffffffff;
    memset(local_448,0,0x400);
    LVar1 = RegCreateKeyExA((HKEY)0xffffffff80000002,"SOFTWARE\\Niagara4",0,(LPSTR)0x0,0,0xf013f,
                            (LPSECURITY_ATTRIBUTES)0x0,&local_478,local_450);
    pBVar8 = pBVar7;
    pBVar5 = pBVar10;
    if (LVar1 == 0) {
      if (param_4 == 0) {
        RegDeleteValueA(local_478,lpValueName);
        pBVar8 = pBVar10;
      }
      else {
        pBVar5 = (BYTE *)(**(code **)(*param_1 + 0x5c0))(param_1,param_4,0);
        DVar2 = (**(code **)(*param_1 + 0x558))(param_1,param_4);
        local_460.cbData = DVar2;
        local_460.pbData = pBVar5;
        iVar3 = DpapiHelper::encrypt(&local_460,&local_470,param_5 == '\x01','\x01');
        if (iVar3 == 0) {
          FUN_1800052d0(local_448,0x400,0xffffffffffffffff,"Error encrypting registry value\n");
        }
        else {
          LVar1 = RegSetValueExA(local_478,lpValueName,0,3,local_470.pbData,local_470.cbData);
          pBVar8 = pBVar10;
          if (LVar1 != 0) {
            FUN_1800052d0(local_448,0x400,0xffffffffffffffff,
                          "Error setting the registry value (error = %ld)\n");
            pBVar8 = pBVar7;
          }
        }
      }
    }
    else {
      FUN_1800052d0(local_448,0x400,0xffffffffffffffff,"Error creating registry key (error = %ld)\n"
                   );
      local_478 = (HKEY)0x0;
    }
    if (lpValueName != (LPCSTR)0x0) {
      (**(code **)(*param_1 + 0x550))(param_1,param_3,lpValueName);
    }
    if (pBVar5 != (BYTE *)0x0) {
      (**(code **)(*param_1 + 0x600))(param_1,param_4,pBVar5,2);
    }
    if (local_478 != (HKEY)0x0) {
      RegCloseKey(local_478);
      local_478 = (HKEY)0x0;
    }
    if (local_470.pbData != (BYTE *)0x0) {
      p_Var9 = &local_470;
      for (lVar6 = 0x10; lVar6 != 0; lVar6 = lVar6 + -1) {
        *(undefined1 *)&p_Var9->cbData = 0;
        p_Var9 = (_CRYPTOAPI_BLOB *)((longlong)&p_Var9->cbData + 1);
      }
      LocalFree(local_470.pbData);
    }
    if ((int)pBVar8 != 0) {
      uVar4 = (**(code **)(*param_1 + 0x30))(param_1,"java/lang/SecurityException");
      (**(code **)(*param_1 + 0x70))(param_1,uVar4,local_448);
    }
  }
  FUN_18000ce30(local_48 ^ (ulonglong)auStackY_4c8);
  return;
}


