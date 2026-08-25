/* rsdd ghidra C export
   program : njre.dll
   filter  : getHostId|getVolume|getHostProduct|CachedProductIdKey|getOrCreate
*/

/* ---- generateNewCachedProductIdKey @ 180004c70 ---- */

/* private: int __cdecl NreWin32::generateNewCachedProductIdKey(char * __ptr64,int) __ptr64 */

int __thiscall NreWin32::generateNewCachedProductIdKey(NreWin32 *this,char *param_1,int param_2)

{
  int iVar1;
  undefined1 auStackY_48 [32];
  ulong local_18 [2];
  ulonglong local_10;
  
                    /* 0x4c70  45  ?generateNewCachedProductIdKey@NreWin32@@AEAAHPEADH@Z */
  local_10 = DAT_18000f178 ^ (ulonglong)auStackY_48;
  local_18[0] = param_2;
  iVar1 = getRegWinCurVerImpl(this,"Windows NT","ProductId",(uchar *)param_1,local_18);
  if (iVar1 != 0) {
    getRegWinCurVerImpl(this,"Windows","ProductId",(uchar *)param_1,local_18);
  }
  iVar1 = FUN_180007d40(local_10 ^ (ulonglong)auStackY_48);
  return iVar1;
}



/* ---- getHostId @ 180004ec0 ---- */

/* public: virtual int __cdecl NreWin32::getHostId(char * __ptr64,unsigned int) __ptr64 */

int __thiscall NreWin32::getHostId(NreWin32 *this,char *param_1,uint param_2)

{
  int iVar1;
  undefined8 uVar2;
  longlong lVar3;
  longlong lVar4;
  undefined8 uVar5;
  longlong lVar6;
  undefined4 in_register_00000084;
  undefined8 in_R9;
  char *pcVar7;
  longlong lVar8;
  undefined1 auStackY_4c8 [32];
  ulong local_458 [4];
  uchar local_448 [256];
  char local_348 [256];
  char local_248 [256];
  char local_148 [256];
  ulonglong local_48;
  
                    /* 0x4ec0  50  ?getHostId@NreWin32@@UEAAHPEADI@Z */
  uVar5 = CONCAT44(in_register_00000084,param_2);
  local_48 = DAT_18000f178 ^ (ulonglong)auStackY_4c8;
  if (DAT_18000f3c8 != 0) {
    uVar2 = __acrt_iob_func(2);
    FUN_1800049f0(uVar2,">>> hostid.debug >>>\n",uVar5,in_R9);
  }
  builtin_strncpy(local_248,"key",4);
  memset(local_248 + 4,0,0xfb);
  builtin_memcpy(local_448,"owner",6);
  memset(local_448 + 6,0,0xf9);
  builtin_strncpy(local_148,"product",8);
  memset(local_148 + 8,0,0xf7);
  builtin_strncpy(local_348,"volume",7);
  memset(local_348 + 7,0,0xf8);
  getOrCreateHiddenKey(this,local_248,0xff);
  local_458[0] = 0xff;
  iVar1 = getRegWinCurVerImpl(this,"Windows NT","RegisteredOwner",local_448,local_458);
  if (iVar1 != 0) {
    getRegWinCurVerImpl(this,"Windows","RegisteredOwner",local_448,local_458);
  }
  getOrCreateCachedProductIdKey(this,local_148,0xff);
  getVolume(this,local_348,0xff);
  lVar6 = -1;
  do {
    lVar3 = lVar6 + 1;
    lVar8 = lVar6 + 1;
    lVar6 = lVar3;
  } while (local_248[lVar8] != '\0');
  lVar8 = 0;
  iVar1 = (int)lVar3;
  lVar6 = lVar8;
  if (0 < iVar1) {
    do {
      lVar6 = lVar6 + 1;
    } while (lVar6 < iVar1);
  }
  lVar6 = -1;
  do {
    lVar4 = lVar6 + 1;
    lVar3 = lVar6 + 1;
    lVar6 = lVar4;
  } while (local_448[lVar3] != '\0');
  iVar1 = (int)lVar4;
  lVar6 = lVar8;
  if (0 < iVar1) {
    do {
      lVar6 = lVar6 + 1;
    } while (lVar6 < iVar1);
  }
  lVar6 = -1;
  do {
    lVar4 = lVar6 + 1;
    lVar3 = lVar6 + 1;
    lVar6 = lVar4;
  } while (local_148[lVar3] != '\0');
  iVar1 = (int)lVar4;
  lVar6 = lVar8;
  if (0 < iVar1) {
    do {
      lVar6 = lVar6 + 1;
    } while (lVar6 < iVar1);
  }
  lVar6 = -1;
  do {
    lVar4 = lVar6 + 1;
    lVar3 = lVar6 + 1;
    lVar6 = lVar4;
  } while (local_348[lVar3] != '\0');
  iVar1 = (int)lVar4;
  if (0 < iVar1) {
    do {
      lVar8 = lVar8 + 1;
    } while (lVar8 < iVar1);
  }
  pcVar7 = "%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X";
  FUN_180004980(param_1,(ulonglong)param_2,0xffffffffffffffff,
                "%s-%02X%02X-%02X%02X-%02X%02X-%02X%02X");
  if (DAT_18000f3c8 != 0) {
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"  prefix  = \'%s\'\n",&DAT_18000a6a0,pcVar7);
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"  hidden  = \'%s\'\n",local_248,pcVar7);
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"  owner   = \'%s\'\n",local_448,pcVar7);
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"  product = \'%s\'\n",local_148,pcVar7);
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"  volume  = \'%s\'\n",local_348,pcVar7);
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"  result  = \'%s\'\n",param_1,pcVar7);
    uVar5 = __acrt_iob_func(2);
    FUN_1800049f0(uVar5,"<<< hostid.debug <<<\n",param_1,pcVar7);
  }
  iVar1 = FUN_180007d40(local_48 ^ (ulonglong)auStackY_4c8);
  return iVar1;
}



/* ---- getHostProduct @ 180005490 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: virtual int __cdecl NreWin32::getHostProduct(char * __ptr64,unsigned int) __ptr64 */

int __thiscall NreWin32::getHostProduct(NreWin32 *this,char *param_1,uint param_2)

{
  longlong *plVar1;
  char *_Src;
  
                    /* 0x5490  54  ?getHostProduct@NreWin32@@UEAAHPEADI@Z */
  plVar1 = FUN_1800013c0();
  if (plVar1 != (longlong *)0x0) {
    _Src = (char *)(**(code **)*plVar1)(plVar1,"brand.product",&DAT_180009754);
    strncpy_s(param_1,(ulonglong)param_2,_Src,0xffffffffffffffff);
  }
  if (*param_1 == '\0') {
    strncpy_s(param_1,(ulonglong)param_2,"",0xffffffffffffffff);
  }
  return 0;
}



/* ---- getOrCreateCachedProductIdKey @ 180005f00 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* private: void __cdecl NreWin32::getOrCreateCachedProductIdKey(char * __ptr64,int) __ptr64 */

void __thiscall NreWin32::getOrCreateCachedProductIdKey(NreWin32 *this,char *param_1,int param_2)

{
  int iVar1;
  longlong *plVar2;
  longlong lVar3;
  undefined8 uVar4;
  longlong lVar5;
  longlong lVar6;
  undefined1 *puVar7;
  ulonglong uVar8;
  
                    /* 0x5f00  64  ?getOrCreateCachedProductIdKey@NreWin32@@AEAAXPEADH@Z */
  uVar8 = (ulonglong)(uint)param_2;
  iVar1 = readCachedProductIdKey(this,param_1,param_2);
  if (iVar1 != 0) {
    plVar2 = FUN_180001470();
    if (plVar2 != (longlong *)0x0) {
      lVar3 = (**(code **)*plVar2)(plVar2,"disableHostIdGeneration","false");
      puVar7 = &DAT_180009f40;
      lVar5 = 0;
      while( true ) {
        lVar6 = lVar5 + 1;
        if (*(char *)(lVar3 + lVar5) != (&DAT_180009f40)[lVar5]) break;
        lVar5 = lVar6;
        if (lVar6 == 5) {
          uVar4 = __acrt_iob_func(2);
          FUN_1800049f0(uVar4,"ERROR: Host Id cannot be found/generated.\n",puVar7,plVar2);
                    /* WARNING: Subroutine does not return */
          exit(0xf9);
        }
      }
    }
    iVar1 = generateNewCachedProductIdKey(this,param_1,param_2);
    if (iVar1 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_1800049f0(uVar4,"ERROR: Can not generate product id.\n",uVar8,plVar2);
                    /* WARNING: Subroutine does not return */
      exit(0xf9);
    }
    iVar1 = saveCachedProductIdKey(this,param_1);
    if (iVar1 != 0) {
      uVar4 = __acrt_iob_func(2);
      FUN_1800049f0(uVar4,"ERROR: Can not save product id.\n",uVar8,plVar2);
                    /* WARNING: Subroutine does not return */
      exit(0xf9);
    }
  }
  return;
}



/* ---- getOrCreateHiddenKey @ 180006020 ---- */

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
  
                    /* 0x6020  65  ?getOrCreateHiddenKey@NreWin32@@AEAAXPEADH@Z */
  uVar5 = CONCAT44(in_register_00000084,param_2);
  uVar9 = (ulonglong)(uint)param_2;
  iVar1 = migrateHiddenKey(this);
  if (iVar1 != 0) {
    uVar2 = __acrt_iob_func(2);
    FUN_1800049f0(uVar2,"ERROR: Can not migrate lk.\n",uVar5,in_R9);
                    /* WARNING: Subroutine does not return */
    exit(0xf9);
  }
  iVar1 = readHiddenKey(this,param_1,param_2);
  if (iVar1 != 0) {
    plVar3 = FUN_180001470();
    if (plVar3 != (longlong *)0x0) {
      lVar4 = (**(code **)*plVar3)(plVar3,"disableHostIdGeneration","false");
      puVar8 = &DAT_180009f40;
      lVar6 = 0;
      while( true ) {
        lVar7 = lVar6 + 1;
        if (*(char *)(lVar4 + lVar6) != (&DAT_180009f40)[lVar6]) break;
        lVar6 = lVar7;
        if (lVar7 == 5) {
          uVar5 = __acrt_iob_func(2);
          FUN_1800049f0(uVar5,"ERROR: Host Id cannot be found/generated.\n",puVar8,plVar3);
                    /* WARNING: Subroutine does not return */
          exit(0xf9);
        }
      }
    }
    iVar1 = generateNewKey(this,param_1,param_2);
    if (iVar1 != 0) {
      uVar5 = __acrt_iob_func(2);
      FUN_1800049f0(uVar5,"ERROR: Can not generate lk.\n",uVar9,plVar3);
                    /* WARNING: Subroutine does not return */
      exit(0xf9);
    }
    iVar1 = saveHiddenKey(this,param_1);
    if (iVar1 != 0) {
      uVar5 = __acrt_iob_func(2);
      FUN_1800049f0(uVar5,"ERROR: Can not save lk.\n",uVar9,plVar3);
                    /* WARNING: Subroutine does not return */
      exit(0xf9);
    }
  }
  return;
}



/* ---- getVolume @ 1800064d0 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* private: void __cdecl NreWin32::getVolume(char * __ptr64,int) __ptr64 */

void __thiscall NreWin32::getVolume(NreWin32 *this,char *param_1,int param_2)

{
  BOOL BVar1;
  undefined8 uVar2;
  undefined8 uVar3;
  LPDWORD lpVolumeSerialNumber;
  undefined1 auStackY_378 [32];
  DWORD local_338 [4];
  CHAR local_328 [256];
  CHAR local_228 [256];
  CHAR local_128 [256];
  ulonglong local_28;
  
                    /* 0x64d0  74  ?getVolume@NreWin32@@AEAAXPEADH@Z */
  local_28 = DAT_18000f178 ^ (ulonglong)auStackY_378;
  builtin_memcpy(local_328,"c:\\",4);
  memset(local_328 + 4,0,0xfc);
  memset(local_128,0,0x100);
  local_338[0] = 0;
  local_338[1] = 0;
  memset(local_228,0,0x100);
  lpVolumeSerialNumber = local_338;
  uVar3 = 0xff;
  BVar1 = GetVolumeInformationA
                    (local_328,local_128,0xff,lpVolumeSerialNumber,local_338 + 1,local_338 + 2,
                     local_228,0x100);
  if (BVar1 == 0) {
    uVar2 = __acrt_iob_func(2);
    FUN_1800049f0(uVar2,"ERROR Can not retrieve volume information: ",uVar3,lpVolumeSerialNumber);
    (**(code **)(*(longlong *)this + 0x80))(this);
  }
  else {
    FUN_180004980(param_1,(longlong)param_2,0xffffffffffffffff,&DAT_18000acb4);
  }
  FUN_180007d40(local_28 ^ (ulonglong)auStackY_378);
  return;
}



/* ---- readCachedProductIdKey @ 180006c70 ---- */

/* private: int __cdecl NreWin32::readCachedProductIdKey(char * __ptr64,int) __ptr64 */

int __thiscall NreWin32::readCachedProductIdKey(NreWin32 *this,char *param_1,int param_2)

{
  ulong uVar1;
  int iVar2;
  undefined8 uVar3;
  undefined8 uVar4;
  undefined8 uVar5;
  undefined1 auStackY_68 [32];
  HKEY local_38;
  DWORD local_30 [2];
  ulonglong local_28;
  
                    /* 0x6c70  96  ?readCachedProductIdKey@NreWin32@@AEAAHPEADH@Z */
  local_28 = DAT_18000f178 ^ (ulonglong)auStackY_68;
  uVar4 = 0;
  uVar5 = 1;
  uVar1 = RegOpenKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Niagara4_18000f138,0,1,&local_38);
  if (uVar1 == 0) {
    uVar5 = 0;
    uVar4 = 0;
    local_30[0] = param_2;
    uVar1 = RegQueryValueExA(local_38,PTR_DAT_18000f140,(LPDWORD)0x0,(LPDWORD)0x0,(LPBYTE)param_1,
                             local_30);
    if (uVar1 == 0) {
      RegCloseKey(local_38);
    }
    else {
      if (DAT_18000f3cc != 0) {
        uVar3 = __acrt_iob_func(2);
        FUN_1800049f0(uVar3,"ERROR NreWin32.RCPIDK.RQVE: ",uVar4,uVar5);
        printError(this,uVar1);
      }
      RegCloseKey(local_38);
    }
  }
  else if (DAT_18000f3cc != 0) {
    uVar3 = __acrt_iob_func(2);
    FUN_1800049f0(uVar3,"ERROR NreWin32.RCPIDK.ROKE: ",uVar4,uVar5);
    printError(this,uVar1);
  }
  iVar2 = FUN_180007d40(local_28 ^ (ulonglong)auStackY_68);
  return iVar2;
}



/* ---- saveCachedProductIdKey @ 1800070d0 ---- */

/* private: int __cdecl NreWin32::saveCachedProductIdKey(char * __ptr64) __ptr64 */

int __thiscall NreWin32::saveCachedProductIdKey(NreWin32 *this,char *param_1)

{
  ulong uVar1;
  int iVar2;
  undefined8 uVar3;
  longlong lVar4;
  undefined8 uVar5;
  undefined8 uVar6;
  undefined1 auStackY_78 [32];
  HKEY local_28;
  DWORD local_20 [2];
  ulonglong local_18;
  
                    /* 0x70d0  98  ?saveCachedProductIdKey@NreWin32@@AEAAHPEAD@Z */
  local_18 = DAT_18000f178 ^ (ulonglong)auStackY_78;
  uVar6 = 0;
  uVar5 = 0;
  uVar1 = RegCreateKeyExA((HKEY)0xffffffff80000002,PTR_s_SOFTWARE_Niagara4_18000f138,0,(LPSTR)0x0,0,
                          0xf003f,(LPSECURITY_ATTRIBUTES)0x0,&local_28,local_20);
  if (uVar1 == 0) {
    lVar4 = -1;
    do {
      lVar4 = lVar4 + 1;
    } while (param_1[lVar4] != '\0');
    uVar6 = 1;
    uVar5 = 0;
    uVar1 = RegSetValueExA(local_28,PTR_DAT_18000f140,0,1,(BYTE *)param_1,(DWORD)lVar4);
    if (uVar1 == 0) {
      RegFlushKey(local_28);
      RegCloseKey(local_28);
    }
    else {
      if (DAT_18000f3cc != 0) {
        uVar3 = __acrt_iob_func(2);
        FUN_1800049f0(uVar3,"ERROR NreWin32.SCPIDK.RSVE: ",uVar5,uVar6);
        printError(this,uVar1);
      }
      RegCloseKey(local_28);
    }
  }
  else if (DAT_18000f3cc != 0) {
    uVar3 = __acrt_iob_func(2);
    FUN_1800049f0(uVar3,"ERROR NreWin32.SCPIDK.RCKE: ",uVar5,uVar6);
    printError(this,uVar1);
  }
  iVar2 = FUN_180007d40(local_18 ^ (ulonglong)auStackY_78);
  return iVar2;
}


