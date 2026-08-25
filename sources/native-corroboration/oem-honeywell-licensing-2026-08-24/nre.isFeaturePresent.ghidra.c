/* rsdd ghidra C export
   program : nre.dll
   filter  : isFeaturePresent
*/

/* ---- isFeaturePresent @ 180001f90 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: static bool __cdecl LicenseUtil::isFeaturePresent(char const * __ptr64,char const *
   __ptr64) */

bool __cdecl LicenseUtil::isFeaturePresent(char *param_1,char *param_2)

{
  bool bVar1;
  bool bVar2;
  DirectoryListingEntry *_Memory;
  char cVar3;
  bool bVar4;
  undefined1 uVar5;
  int iVar6;
  long lVar7;
  Nre *pNVar8;
  code *pcVar9;
  DirectoryListing *this;
  ulonglong uVar10;
  char *pcVar11;
  FILE *_File;
  size_t sVar12;
  char *pcVar13;
  size_t _Count;
  undefined1 auStack_428 [32];
  char *local_408;
  DirectoryListingEntry *local_3f8 [2];
  undefined8 local_3e8;
  undefined8 uStack_3e0;
  undefined8 local_3d8;
  undefined8 uStack_3d0;
  undefined8 local_3c8;
  undefined8 uStack_3c0;
  undefined8 local_3b8;
  undefined8 uStack_3b0;
  undefined8 local_3a8;
  undefined8 uStack_3a0;
  undefined8 local_398;
  undefined8 uStack_390;
  undefined8 local_388;
  undefined8 uStack_380;
  undefined8 local_378;
  undefined8 uStack_370;
  undefined4 local_368;
  char local_358 [256];
  char local_258 [256];
  char local_158 [272];
  ulonglong local_48;
  
                    /* 0x1f90  95  ?isFeaturePresent@LicenseUtil@@SA_NPEBD0@Z */
  local_48 = DAT_18001a1c8 ^ (ulonglong)auStack_428;
  if ((param_1 != (char *)0x0) && (param_2 != (char *)0x0)) {
    memset(local_358,0,0x100);
    memset(local_258,0,0x100);
    FUN_180001ac0(local_358,0x100,"<license vendor=\"%s\"",param_1);
    FUN_180001ac0(local_258,0x100,"<feature name=\"%s\"",param_2);
    local_368 = 0;
    local_3e8 = 0;
    uStack_3e0 = 0;
    local_3d8 = 0;
    uStack_3d0 = 0;
    local_3c8 = 0;
    uStack_3c0 = 0;
    local_3b8 = 0;
    uStack_3b0 = 0;
    local_3a8 = 0;
    uStack_3a0 = 0;
    local_398 = 0;
    uStack_390 = 0;
    local_388 = 0;
    uStack_380 = 0;
    local_378 = 0;
    uStack_370 = 0;
    memset(local_158,0,0x104);
    pNVar8 = Nre::getInstance();
    cVar3 = (**(code **)(*(longlong *)pNVar8 + 0x68))(pNVar8);
    if (cVar3 == '\0') {
      pNVar8 = Nre::getInstance();
      pcVar9 = *(code **)(*(longlong *)pNVar8 + 0x58);
    }
    else {
      pNVar8 = Nre::getInstance();
      pcVar9 = *(code **)(*(longlong *)pNVar8 + 0x60);
    }
    (*pcVar9)(pNVar8,&local_3e8,0x84);
    local_408 = "\\security\\licenses";
    FUN_180001ac0(local_158,0x104,&DAT_18000ee10,&local_3e8);
    bVar1 = false;
    this = NreLib::DirectoryListing::make(local_158);
    local_3f8[0] = (DirectoryListingEntry *)0x0;
    if (this != (DirectoryListing *)0x0) {
      bVar4 = NreLib::DirectoryListing::hasNext(this);
      if (bVar4) {
        do {
          if ((bVar1) ||
             (NreLib::DirectoryListing::next(this,local_3f8),
             local_3f8[0] == (DirectoryListingEntry *)0x0)) break;
          pcVar11 = *(char **)local_3f8[0];
          if ((pcVar11 != (char *)0x0) && (*(longlong *)(local_3f8[0] + 8) != 0)) {
            uVar10 = 0xffffffffffffffff;
            do {
              uVar10 = uVar10 + 1;
            } while (pcVar11[uVar10] != '\0');
            if ((uVar10 < 2) || (*pcVar11 == '.')) {
              bVar4 = false;
            }
            else {
              bVar4 = true;
            }
            pcVar11 = strrchr(pcVar11,0x2e);
            if (pcVar11 == (char *)0x0) {
LAB_1800021c3:
              bVar2 = false;
            }
            else {
              pcVar11 = strrchr(*(char **)local_3f8[0],0x2e);
              iVar6 = _stricmp(pcVar11,".license");
              if (iVar6 != 0) goto LAB_1800021c3;
              bVar2 = true;
            }
            if ((((bVar4) && (bVar2)) &&
                (bVar4 = NreLib::DirectoryListing::isDirectory(*(char **)(local_3f8[0] + 8)), !bVar4
                )) && (_File = fopen(*(char **)(local_3f8[0] + 8),"r"), _File != (FILE *)0x0)) {
              iVar6 = fseek(_File,0,2);
              pcVar11 = (char *)0x0;
              if ((iVar6 == 0) && (lVar7 = ftell(_File), 0 < lVar7)) {
                _Count = (size_t)lVar7;
                iVar6 = fseek(_File,0,0);
                pcVar11 = (char *)0x0;
                if ((iVar6 != 0) ||
                   (pcVar11 = (char *)thunk_FUN_18000ca3c(_Count + 1), pcVar11 == (char *)0x0))
                goto LAB_1800022ba;
                memset(pcVar11,0,_Count + 1);
                sVar12 = fread(pcVar11,1,_Count,_File);
                if (sVar12 != _Count) goto LAB_1800022ba;
                pcVar11[sVar12] = '\0';
                pcVar13 = strstr(pcVar11,local_358);
                if ((pcVar13 == (char *)0x0) ||
                   (pcVar13 = strstr(pcVar11,local_258), pcVar13 == (char *)0x0))
                goto LAB_1800022ba;
                bVar1 = true;
                fclose(_File);
              }
              else {
LAB_1800022ba:
                fclose(_File);
                if (pcVar11 == (char *)0x0) goto LAB_1800022d5;
              }
              free(pcVar11);
            }
          }
LAB_1800022d5:
          _Memory = local_3f8[0];
          if (local_3f8[0] != (DirectoryListingEntry *)0x0) {
            NreLib::DirectoryListingEntry::~DirectoryListingEntry(local_3f8[0]);
            free(_Memory);
          }
          local_3f8[0] = (DirectoryListingEntry *)0x0;
          bVar4 = NreLib::DirectoryListing::hasNext(this);
        } while (bVar4);
      }
      NreLib::DirectoryListing::~DirectoryListing(this);
      free(this);
    }
  }
  uVar5 = FUN_18000ce30(local_48 ^ (ulonglong)auStack_428);
  return (bool)uVar5;
}


