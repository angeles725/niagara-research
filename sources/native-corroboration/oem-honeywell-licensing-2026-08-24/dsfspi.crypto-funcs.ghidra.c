/* rsdd ghidra C export
   program : dsfspi.dll
   filter  : nativeVerify|nativeSign|nativeInitVerify|nativeReset|parseDSA|parseDER|createDSASignature|verify|initVerify|initSign
*/

/* ---- nativeReset @ 180027dc0 ---- */

/* public: virtual enum enum_errDescrValues __cdecl DsfSha1MessageDigest::nativeReset(void) __ptr64
    */

enum_errDescrValues __thiscall DsfSha1MessageDigest::nativeReset(DsfSha1MessageDigest *this)

{
  undefined4 *puVar1;
  undefined8 uVar2;
  
                    /* 0x27dc0  87
                       ?nativeReset@DsfSha1MessageDigest@@UEAA?AW4enum_errDescrValues@@XZ */
  if (*(void **)(this + 8) != (void *)0x0) {
    free(*(void **)(this + 8));
  }
  puVar1 = malloc(0x68);
  *(undefined4 **)(this + 8) = puVar1;
  uVar2 = FUN_180021980(puVar1);
  return (enum_errDescrValues)uVar2;
}



/* ---- createDSASignature @ 180028580 ---- */

/* private: enum enum_errDescrValues __cdecl DsfSha1WithDsaSignature::createDSASignature(struct
   vlong * __ptr64,struct vlong * __ptr64,unsigned char * __ptr64 * __ptr64,unsigned int * __ptr64)
   __ptr64 */

enum_errDescrValues __thiscall
DsfSha1WithDsaSignature::createDSASignature
          (DsfSha1WithDsaSignature *this,vlong *param_1,vlong *param_2,uchar **param_3,uint *param_4
          )

{
  enum_errDescrValues eVar1;
  undefined8 uVar2;
  byte *_Memory;
  byte *_Memory_00;
  size_t sVar3;
  undefined1 auStackY_88 [32];
  uint local_58;
  uint local_54;
  longlong *local_50;
  undefined8 local_48;
  undefined8 local_40;
  ulonglong local_38;
  
                    /* 0x28580  56
                       ?createDSASignature@DsfSha1WithDsaSignature@@AEAA?AW4enum_errDescrValues@@PEAUvlong@@0PEAPEAEPEAI@Z
                        */
  local_38 = DAT_180054f48 ^ (ulonglong)auStackY_88;
  uVar2 = FUN_1800096a0((longlong *)param_1,(undefined1 *)0x0,&local_58);
  if ((int)uVar2 == 0) {
    sVar3 = (size_t)(local_58 + 1);
    if (local_58 == 0xffffffff) {
      sVar3 = 0xffffffffffffffff;
    }
    _Memory = malloc(sVar3);
    if (_Memory != (byte *)0x0) {
      *_Memory = 0;
      uVar2 = FUN_1800096a0((longlong *)param_1,_Memory + 1,&local_58);
      if (((int)uVar2 == 0) &&
         (uVar2 = FUN_1800096a0((longlong *)param_2,(undefined1 *)0x0,&local_54), (int)uVar2 == 0))
      {
        sVar3 = (size_t)(local_54 + 1);
        if (local_54 == 0xffffffff) {
          sVar3 = 0xffffffffffffffff;
        }
        _Memory_00 = malloc(sVar3);
        if (_Memory_00 == (byte *)0x0) {
          free(_Memory);
        }
        else {
          *_Memory_00 = 0;
          uVar2 = FUN_1800096a0((longlong *)param_2,_Memory_00 + 1,&local_54);
          if (((int)uVar2 == 0) && (uVar2 = FUN_180001410(0,0x30,0,0,&local_50), (int)uVar2 == 0)) {
            uVar2 = FUN_180001220((longlong)local_50,local_58 + 1,_Memory,&local_48);
            if (((int)uVar2 == 0) &&
               (uVar2 = FUN_180001220((longlong)local_50,local_54 + 1,_Memory_00,&local_40),
               (int)uVar2 == 0)) {
              FUN_180001790((longlong)local_50,param_3,param_4);
            }
            free(_Memory);
            free(_Memory_00);
            FUN_180005920(local_50);
          }
          else {
            free(_Memory);
            free(_Memory_00);
          }
        }
      }
      else {
        free(_Memory);
      }
    }
  }
  eVar1 = FUN_18002c590(local_38 ^ (ulonglong)auStackY_88);
  return eVar1;
}



/* ---- initSign @ 180028740 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: void __cdecl DsfSha1WithDsaSignature::initSign(struct JNIEnv_ * __ptr64,class _jobject *
   __ptr64,class _jbyteArray * __ptr64) __ptr64 */

void __thiscall
DsfSha1WithDsaSignature::initSign
          (DsfSha1WithDsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3)

{
  undefined4 uVar1;
  enum_errDescrValues eVar2;
  longlong lVar3;
  
                    /* 0x28740  73
                       ?initSign@DsfSha1WithDsaSignature@@QEAAXPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@@Z
                        */
  reset(this,param_1,param_2);
  uVar1 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
  lVar3 = (**(code **)(*(longlong *)param_1 + 0x6f0))(param_1,param_3,0);
  eVar2 = FUN_180017e60(lVar3,uVar1,(int *)(this + 8));
  (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3,lVar3,0);
  if (eVar2 != 0) {
    DsfObject::throwException
              ((DsfObject *)(this + 8),param_1,"java/security/ProviderException",
               "unable to read dsa private key",eVar2);
  }
  *(undefined4 *)(this + 0x28) = 1;
  return;
}



/* ---- initVerify @ 180028810 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: void __cdecl DsfSha1WithDsaSignature::initVerify(struct JNIEnv_ * __ptr64,class _jobject
   * __ptr64,class _jbyteArray * __ptr64) __ptr64 */

void __thiscall
DsfSha1WithDsaSignature::initVerify
          (DsfSha1WithDsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3)

{
  uint uVar1;
  enum_errDescrValues eVar2;
  uchar *puVar3;
  
                    /* 0x28810  75
                       ?initVerify@DsfSha1WithDsaSignature@@QEAAXPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@@Z
                        */
  reset(this,param_1,param_2);
  uVar1 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
  puVar3 = (uchar *)(**(code **)(*(longlong *)param_1 + 0x6f0))(param_1,param_3,0);
  eVar2 = parseDSAPublicKey(this,puVar3,uVar1,(AsymmetricKey *)(this + 8));
  (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3,puVar3,0);
  if (eVar2 == 0) {
    *(undefined4 *)(this + 0x28) = 2;
  }
  else {
    DsfObject::throwException
              ((DsfObject *)(this + 8),param_1,"java/security/ProviderException",
               "unable to read dsa public key",eVar2);
  }
  return;
}



/* ---- parseDERInteger @ 1800288e0 ---- */

/* private: enum enum_errDescrValues __cdecl DsfSha1WithDsaSignature::parseDERInteger(unsigned char
   const * __ptr64,unsigned int,unsigned char * __ptr64 * __ptr64,unsigned int * __ptr64) __ptr64 */

enum_errDescrValues __thiscall
DsfSha1WithDsaSignature::parseDERInteger
          (DsfSha1WithDsaSignature *this,uchar *param_1,uint param_2,uchar **param_3,uint *param_4)

{
  uint uVar1;
  longlong lVar2;
  int iVar3;
  enum_errDescrValues eVar4;
  undefined8 uVar5;
  void *_Src;
  uchar *_Dst;
  undefined1 auStack_98 [32];
  undefined8 local_78;
  undefined8 uStack_70;
  longlong *local_68 [2];
  undefined8 local_58;
  undefined8 uStack_50;
  undefined8 local_48 [2];
  ulonglong local_38;
  
                    /* 0x288e0  98
                       ?parseDERInteger@DsfSha1WithDsaSignature@@AEAA?AW4enum_errDescrValues@@PEBEIPEAPEAEPEAI@Z
                        */
  local_38 = DAT_180054f48 ^ (ulonglong)auStack_98;
  local_68[0] = (longlong *)0x0;
  FUN_1800039e0(local_48,param_2,param_1);
  FUN_180002d20(&local_58,local_48);
  local_78 = local_58;
  uStack_70 = uStack_50;
  iVar3 = FUN_180001d60(&local_78,local_68);
  if (-1 < iVar3) {
    lVar2 = local_68[0][1];
    uVar5 = FUN_1800024e0(lVar2,2);
    if (-1 < (int)uVar5) {
      uVar1 = *(uint *)(lVar2 + 0x28);
      local_78 = local_58;
      uStack_70 = uStack_50;
      _Src = (void *)FUN_180002d70(&local_78);
      if (_Src != (void *)0x0) {
        _Dst = malloc((ulonglong)uVar1);
        *param_3 = _Dst;
        memcpy(_Dst,_Src,(ulonglong)uVar1);
        *param_4 = uVar1;
        local_78 = local_58;
        uStack_70 = uStack_50;
        FUN_180002db0(&local_78);
      }
    }
  }
  if (local_68[0] != (longlong *)0x0) {
    FUN_180005920(local_68[0]);
  }
  eVar4 = FUN_18002c590(local_38 ^ (ulonglong)auStack_98);
  return eVar4;
}



/* ---- parseDSAPublicKey @ 180028a50 ---- */

/* private: enum enum_errDescrValues __cdecl DsfSha1WithDsaSignature::parseDSAPublicKey(unsigned
   char const * __ptr64,unsigned int,struct AsymmetricKey * __ptr64) __ptr64 */

enum_errDescrValues __thiscall
DsfSha1WithDsaSignature::parseDSAPublicKey
          (DsfSha1WithDsaSignature *this,uchar *param_1,uint param_2,AsymmetricKey *param_3)

{
  uint uVar1;
  longlong lVar2;
  enum_errDescrValues eVar3;
  undefined8 uVar4;
  longlong lVar5;
  ulonglong uVar6;
  undefined8 *puVar7;
  uchar *puVar8;
  uchar *puVar9;
  undefined1 auStackY_108 [32];
  undefined8 local_b8;
  longlong local_a8 [2];
  undefined8 local_98 [2];
  uint local_88;
  int local_84;
  int local_80;
  int local_7c;
  uchar *local_78;
  longlong *local_70;
  undefined4 local_68;
  undefined4 uStack_64;
  undefined8 local_58 [2];
  ulonglong local_48;
  uchar *_Memory;
  
                    /* 0x28a50  99
                       ?parseDSAPublicKey@DsfSha1WithDsaSignature@@AEAA?AW4enum_errDescrValues@@PEBEIPEAUAsymmetricKey@@@Z
                        */
  local_48 = DAT_180054f48 ^ (ulonglong)auStackY_108;
  _Memory = (uchar *)0x0;
  puVar8 = (uchar *)0x0;
  local_70 = (longlong *)0x0;
  local_78 = (uchar *)0x0;
  puVar9 = (uchar *)0x0;
  local_b8 = this;
  if ((param_1 == (uchar *)0x0) || (param_3 == (AsymmetricKey *)0x0)) goto LAB_180028e0f;
  FUN_1800039e0(local_58,param_2,param_1);
  FUN_180002d20((undefined8 *)&local_68,local_58);
  local_a8[0] = CONCAT44(uStack_64,local_68);
  eVar3 = FUN_180001d60(local_a8,&local_70);
  if (-1 < (int)eVar3) {
    lVar2 = local_70[1];
    uVar4 = FUN_1800024e0(lVar2,0x10);
    if ((int)uVar4 < 0) {
      eVar3 = 0xffffe24d;
    }
    else {
      lVar2 = *(longlong *)(lVar2 + 8);
      uVar4 = FUN_1800024e0(lVar2,0x10);
      if ((int)uVar4 < 0) {
        eVar3 = 0xffffe24d;
      }
      else {
        lVar2 = *(longlong *)(lVar2 + 8);
        uVar4 = FUN_1800024e0(lVar2,6);
        if ((int)uVar4 < 0) {
          eVar3 = 0xffffe24d;
          _Memory = puVar8;
        }
        else {
          lVar2 = *(longlong *)(lVar2 + 0x10);
          uVar4 = FUN_1800024e0(lVar2,0x10);
          if ((int)uVar4 < 0) {
            eVar3 = 0xffffe24d;
            _Memory = puVar8;
          }
          else {
            lVar2 = *(longlong *)(lVar2 + 8);
            uVar4 = FUN_1800024e0(lVar2,2);
            if ((int)uVar4 < 0) {
              eVar3 = 0xffffe24d;
              _Memory = puVar8;
            }
            else {
              local_7c = *(int *)(lVar2 + 0x28);
              local_a8[0] = CONCAT44(uStack_64,local_68);
              lVar5 = FUN_180002d70(local_a8);
              if (lVar5 == 0) {
                eVar3 = 0xffffe82b;
              }
              else {
                lVar2 = *(longlong *)(lVar2 + 0x10);
                uVar4 = FUN_1800024e0(lVar2,2);
                if ((int)uVar4 < 0) {
                  eVar3 = 0xffffe24d;
                  puVar7 = &local_b8;
                  local_b8 = (DsfSha1WithDsaSignature *)CONCAT44(uStack_64,local_68);
                }
                else {
                  local_80 = *(int *)(lVar2 + 0x28);
                  local_a8[0] = FUN_180002d70(local_a8);
                  if (local_a8[0] == 0) {
                    local_b8 = (DsfSha1WithDsaSignature *)CONCAT44(uStack_64,local_68);
                    puVar7 = &local_b8;
                    eVar3 = 0xffffe82b;
                  }
                  else {
                    puVar7 = *(undefined8 **)(lVar2 + 0x10);
                    uVar4 = FUN_1800024e0((longlong)puVar7,2);
                    if ((int)uVar4 < 0) {
                      eVar3 = 0xffffe24d;
                      puVar8 = _Memory;
                      puVar9 = _Memory;
                    }
                    else {
                      local_84 = *(int *)(puVar7 + 5);
                      puVar8 = (uchar *)FUN_180002d70(local_98);
                      if (puVar8 == (uchar *)0x0) {
                        eVar3 = 0xffffe82b;
                        puVar9 = _Memory;
                      }
                      else {
                        lVar2 = *(longlong *)*puVar7;
                        uVar4 = FUN_1800024e0(lVar2,0x10);
                        if ((int)uVar4 < 0) {
                          eVar3 = 0xffffe24d;
                        }
                        else {
                          lVar2 = *(longlong *)(lVar2 + 0x10);
                          uVar4 = FUN_1800024e0(lVar2,3);
                          if ((int)uVar4 < 0) {
                            eVar3 = 0xffffe24d;
                          }
                          else {
                            uVar1 = *(uint *)(lVar2 + 0x28);
                            puVar9 = (uchar *)FUN_180002d70(local_98);
                            if (puVar9 == (uchar *)0x0) {
                              eVar3 = 0xffffe82b;
                            }
                            else {
                              eVar3 = parseDERInteger(local_b8,puVar9,uVar1,&local_78,&local_88);
                              _Memory = local_78;
                              if (eVar3 == 0) {
                                uVar6 = FUN_18001db20((int *)param_3,(longlong *)0x0);
                                _Memory = local_78;
                                eVar3 = (enum_errDescrValues)uVar6;
                                if (-1 < (int)eVar3) {
                                  uVar6 = FUN_180013cd0(*(longlong **)(param_3 + 8),lVar5,local_7c,
                                                        local_a8[0],local_80,(longlong)puVar8,
                                                        local_84,(longlong)local_78,local_88,
                                                        (longlong *)0x0);
                                  eVar3 = (enum_errDescrValues)uVar6;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                    local_b8 = (DsfSha1WithDsaSignature *)CONCAT44(uStack_64,local_68);
                    FUN_180002db0(&local_b8);
                    FUN_180002db0(local_98);
                    if (puVar8 != (uchar *)0x0) {
                      FUN_180002db0(local_98);
                    }
                    if (puVar9 == (uchar *)0x0) goto LAB_180028dd4;
                    puVar7 = local_98;
                  }
                }
                FUN_180002db0(puVar7);
              }
            }
          }
        }
      }
    }
  }
LAB_180028dd4:
  if (local_70 != (longlong *)0x0) {
    FUN_180005920(local_70);
  }
  if (_Memory != (uchar *)0x0) {
    free(_Memory);
  }
  if ((int)eVar3 < 0) {
    FUN_18001df30((int *)param_3,(longlong *)0x0);
  }
LAB_180028e0f:
  eVar3 = FUN_18002c590(local_48 ^ (ulonglong)auStackY_108);
  return eVar3;
}



/* ---- parseDSASignature @ 180028e30 ---- */

/* private: enum enum_errDescrValues __cdecl DsfSha1WithDsaSignature::parseDSASignature(unsigned
   char * __ptr64,unsigned int,struct vlong * __ptr64 * __ptr64,struct vlong * __ptr64 * __ptr64)
   __ptr64 */

enum_errDescrValues __thiscall
DsfSha1WithDsaSignature::parseDSASignature
          (DsfSha1WithDsaSignature *this,uchar *param_1,uint param_2,vlong **param_3,vlong **param_4
          )

{
  int iVar1;
  int iVar2;
  enum_errDescrValues eVar3;
  undefined8 uVar4;
  longlong lVar5;
  longlong lVar6;
  ulonglong uVar7;
  undefined1 auStack_a8 [32];
  undefined8 local_88;
  undefined8 uStack_80;
  longlong *local_78 [2];
  undefined8 local_68;
  undefined8 uStack_60;
  undefined8 local_58 [2];
  ulonglong local_48;
  
                    /* 0x28e30  100
                       ?parseDSASignature@DsfSha1WithDsaSignature@@AEAA?AW4enum_errDescrValues@@PEAEIPEAPEAUvlong@@1@Z
                        */
  local_48 = DAT_180054f48 ^ (ulonglong)auStack_a8;
  local_78[0] = (longlong *)0x0;
  if (param_1 != (uchar *)0x0) {
    FUN_1800039e0(local_58,param_2,param_1);
    FUN_180002d20(&local_68,local_58);
    local_88 = local_68;
    uStack_80 = uStack_60;
    iVar2 = FUN_180001d60(&local_88,local_78);
    if (iVar2 == 0) {
      lVar6 = local_78[0][1];
      uVar4 = FUN_1800024e0(lVar6,0x10);
      if (-1 < (int)uVar4) {
        lVar6 = *(longlong *)(lVar6 + 8);
        uVar4 = FUN_1800024e0(lVar6,2);
        if (-1 < (int)uVar4) {
          iVar2 = *(int *)(lVar6 + 0x28);
          local_88 = local_68;
          uStack_80 = uStack_60;
          lVar5 = FUN_180002d70(&local_88);
          if (lVar5 == 0) {
            FUN_180005920(local_78[0]);
          }
          else {
            lVar6 = *(longlong *)(lVar6 + 0x10);
            uVar4 = FUN_1800024e0(lVar6,2);
            if ((int)uVar4 < 0) {
              FUN_180005920(local_78[0]);
            }
            else {
              iVar1 = *(int *)(lVar6 + 0x28);
              local_88 = local_68;
              uStack_80 = uStack_60;
              lVar6 = FUN_180002d70(&local_88);
              if (lVar6 == 0) {
                local_88 = local_68;
                uStack_80 = uStack_60;
                FUN_180002db0(&local_88);
                FUN_180005920(local_78[0]);
              }
              else {
                uVar7 = FUN_18000ed90(lVar5,iVar2,(longlong *)param_3,(longlong *)0x0);
                if ((int)uVar7 == 0) {
                  uVar7 = FUN_18000ed90(lVar6,iVar1,(longlong *)param_4,(longlong *)0x0);
                  local_88 = local_68;
                  uStack_80 = uStack_60;
                  if ((int)uVar7 == 0) {
                    FUN_180002db0(&local_88);
                    local_88 = local_68;
                    uStack_80 = uStack_60;
                    FUN_180002db0(&local_88);
                    FUN_180005920(local_78[0]);
                    goto LAB_180029062;
                  }
                }
                FUN_180002db0(&local_88);
                local_88 = local_68;
                uStack_80 = uStack_60;
                FUN_180002db0(&local_88);
                FUN_180005920(local_78[0]);
              }
            }
          }
          goto LAB_180029062;
        }
      }
      FUN_180005920(local_78[0]);
    }
  }
LAB_180029062:
  eVar3 = FUN_18002c590(local_48 ^ (ulonglong)auStack_a8);
  return eVar3;
}



/* ---- verify @ 1800296b0 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: unsigned char __cdecl DsfSha1WithDsaSignature::verify(struct JNIEnv_ * __ptr64,class
   _jobject * __ptr64,class _jbyteArray * __ptr64) __ptr64 */

uchar __thiscall
DsfSha1WithDsaSignature::verify
          (DsfSha1WithDsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3)

{
  uchar uVar1;
  long lVar2;
  
                    /* 0x296b0  126
                       ?verify@DsfSha1WithDsaSignature@@QEAAEPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@@Z
                        */
  lVar2 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
  uVar1 = verify(this,param_1,param_2,param_3,0,lVar2);
  return uVar1;
}



/* ---- verify @ 180029720 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: unsigned char __cdecl DsfSha1WithDsaSignature::verify(struct JNIEnv_ * __ptr64,class
   _jobject * __ptr64,class _jbyteArray * __ptr64,long,long) __ptr64 */

uchar __thiscall
DsfSha1WithDsaSignature::verify
          (DsfSha1WithDsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3,
          long param_4,long param_5)

{
  uchar uVar1;
  uint uVar2;
  int iVar3;
  enum_errDescrValues eVar4;
  void *_Memory;
  ulonglong uVar5;
  uchar *puVar6;
  char *pcVar7;
  undefined1 auStackY_a8 [32];
  vlong *local_78;
  vlong *local_70;
  longlong *local_68;
  int local_60 [2];
  ulonglong local_58;
  
                    /* 0x29720  127
                       ?verify@DsfSha1WithDsaSignature@@QEAAEPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@JJ@Z
                        */
  local_58 = DAT_180054f48 ^ (ulonglong)auStackY_a8;
  local_78 = (vlong *)0x0;
  local_70 = (vlong *)0x0;
  local_68 = (longlong *)0x0;
  if (*(int *)(this + 0x28) == 2) {
    uVar2 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
    if (uVar2 - 0x2c < 5) {
      _Memory = (void *)thunk_FUN_18002c9fc(0x14);
      iVar3 = (**(code **)(**(longlong **)(this + 0x20) + 0x30))
                        (*(longlong **)(this + 0x20),param_1,param_2);
      if (iVar3 == *(int *)(this + 0x2c)) {
        uVar5 = FUN_18000ed90((longlong)_Memory,0x14,(longlong *)&local_68,(longlong *)0x0);
        eVar4 = (enum_errDescrValues)uVar5;
        free(_Memory);
        if (eVar4 == 0) {
          puVar6 = (uchar *)(**(code **)(*(longlong *)param_1 + 0x6f0))(param_1,param_3,0);
          eVar4 = parseDSASignature(this,puVar6,uVar2,&local_78,&local_70);
          (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3,puVar6,0);
          if (eVar4 == 0) {
            eVar4 = FUN_180013dc0(*(undefined8 **)(this + 0x10),local_68,(longlong *)local_78,
                                  (longlong *)local_70,local_60,(longlong *)0x0);
            FUN_180009c50((longlong *)&local_78,(longlong *)0x0);
            FUN_180009c50((longlong *)&local_70,(longlong *)0x0);
            FUN_180009c50((longlong *)&local_68,(longlong *)0x0);
            if (eVar4 == 0) {
              if (local_60[0] == 0) {
                DsfObject::throwException
                          ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                           "error verifying signature");
              }
              else {
                reset(this,param_1,param_2);
              }
            }
            else {
              DsfObject::throwException
                        ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                         "error verifying signature",eVar4);
            }
            goto LAB_180029906;
          }
          pcVar7 = "error parsing signature";
        }
        else {
          pcVar7 = "converting message to vlong";
        }
        DsfObject::throwException
                  ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",pcVar7,eVar4);
        FUN_180009c50((longlong *)&local_78,(longlong *)0x0);
        FUN_180009c50((longlong *)&local_70,(longlong *)0x0);
        FUN_180009c50((longlong *)&local_68,(longlong *)0x0);
      }
      else {
        DsfObject::throwException
                  ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                   "error calculating final digest");
        FUN_180009c50((longlong *)&local_78,(longlong *)0x0);
        FUN_180009c50((longlong *)&local_70,(longlong *)0x0);
        free(_Memory);
      }
    }
    else {
      DsfObject::throwException
                ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                 "invalid signature size");
    }
  }
  else {
    DsfObject::throwException
              ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
               "signature not initialized for verifying");
  }
LAB_180029906:
  uVar1 = FUN_18002c590(local_58 ^ (ulonglong)auStackY_a8);
  return uVar1;
}



/* ---- nativeReset @ 18002a760 ---- */

/* public: virtual enum enum_errDescrValues __cdecl DsfSha256MessageDigest::nativeReset(void)
   __ptr64 */

enum_errDescrValues __thiscall DsfSha256MessageDigest::nativeReset(DsfSha256MessageDigest *this)

{
  undefined4 *puVar1;
  undefined8 uVar2;
  
                    /* 0x2a760  88
                       ?nativeReset@DsfSha256MessageDigest@@UEAA?AW4enum_errDescrValues@@XZ */
  if (*(void **)(this + 8) != (void *)0x0) {
    free(*(void **)(this + 8));
  }
  puVar1 = malloc(0x70);
  *(undefined4 **)(this + 8) = puVar1;
  uVar2 = FUN_180023280(puVar1);
  return (enum_errDescrValues)uVar2;
}



/* ---- initSign @ 18002ae40 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: void __cdecl DsfShaWithRsaSignature::initSign(struct JNIEnv_ * __ptr64,class _jobject *
   __ptr64,class _jbyteArray * __ptr64) __ptr64 */

void __thiscall
DsfShaWithRsaSignature::initSign
          (DsfShaWithRsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3)

{
  undefined4 uVar1;
  enum_errDescrValues eVar2;
  longlong lVar3;
  
                    /* 0x2ae40  74
                       ?initSign@DsfShaWithRsaSignature@@QEAAXPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@@Z
                        */
  reset(this,param_1,param_2);
  uVar1 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
  lVar3 = (**(code **)(*(longlong *)param_1 + 0x6f0))(param_1,param_3,0);
  nativeReset(this);
  eVar2 = FUN_180017e60(lVar3,uVar1,(int *)(this + 8));
  if (eVar2 == 0) {
    *(undefined4 *)(this + 0x28) = 1;
    (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3,lVar3,0);
    *(undefined4 *)(this + 0x28) = 1;
  }
  else {
    (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3);
    DsfObject::throwException
              ((DsfObject *)(this + 8),param_1,"java/security/ProviderException",
               "unable to read rsa private key",eVar2);
  }
  return;
}



/* ---- initVerify @ 18002af30 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: void __cdecl DsfShaWithRsaSignature::initVerify(struct JNIEnv_ * __ptr64,class _jobject *
   __ptr64,class _jbyteArray * __ptr64) __ptr64 */

void __thiscall
DsfShaWithRsaSignature::initVerify
          (DsfShaWithRsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3)

{
  undefined4 uVar1;
  enum_errDescrValues eVar2;
  longlong lVar3;
  
                    /* 0x2af30  76
                       ?initVerify@DsfShaWithRsaSignature@@QEAAXPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@@Z
                        */
  reset(this,param_1,param_2);
  uVar1 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
  lVar3 = (**(code **)(*(longlong *)param_1 + 0x6f0))(param_1,param_3,0);
  nativeReset(this);
  eVar2 = FUN_180017a90(lVar3,uVar1,(int *)(this + 8));
  if (eVar2 == 0) {
    *(undefined4 *)(this + 0x28) = 2;
    (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3,lVar3,0);
    *(undefined4 *)(this + 0x28) = 2;
  }
  else {
    (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3);
    DsfObject::throwException
              ((DsfObject *)(this + 8),param_1,"java/security/ProviderException",
               "unable to read rsa public key",eVar2);
  }
  return;
}



/* ---- nativeInitVerify @ 18002b070 ---- */

/* public: enum enum_errDescrValues __cdecl DsfShaWithRsaSignature::nativeInitVerify(int,unsigned
   char * __ptr64) __ptr64 */

enum_errDescrValues __thiscall
DsfShaWithRsaSignature::nativeInitVerify(DsfShaWithRsaSignature *this,int param_1,uchar *param_2)

{
  enum_errDescrValues eVar1;
  
                    /* 0x2b070  85
                       ?nativeInitVerify@DsfShaWithRsaSignature@@QEAA?AW4enum_errDescrValues@@HPEAE@Z
                        */
  nativeReset(this);
  eVar1 = FUN_180017a90((longlong)param_2,param_1,(int *)(this + 8));
  if (eVar1 == 0) {
    *(undefined4 *)(this + 0x28) = 2;
  }
  return eVar1;
}



/* ---- nativeReset @ 18002b0c0 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* private: void __cdecl DsfShaWithRsaSignature::nativeReset(void) __ptr64 */

void __thiscall DsfShaWithRsaSignature::nativeReset(DsfShaWithRsaSignature *this)

{
  undefined8 *puVar1;
  DsfSha1MessageDigest *this_00;
  longlong *plVar2;
  DsfSha256MessageDigest *this_01;
  
                    /* 0x2b0c0  89  ?nativeReset@DsfShaWithRsaSignature@@AEAAXXZ */
  FUN_18001df30((int *)(this + 8),(longlong *)0x0);
  FUN_18001dba0((undefined4 *)(this + 8));
  puVar1 = *(undefined8 **)(this + 0x20);
  if (puVar1 != (undefined8 *)0x0) {
    (**(code **)*puVar1)(puVar1,1);
  }
  if (*(int *)(this + 0x2c) == 0) {
    this_00 = (DsfSha1MessageDigest *)FUN_18002c9fc(0x10);
    if (this_00 == (DsfSha1MessageDigest *)0x0) {
      plVar2 = (longlong *)0x0;
    }
    else {
      *(undefined8 *)this_00 = 0;
      *(undefined8 *)(this_00 + 8) = 0;
      plVar2 = (longlong *)DsfSha1MessageDigest::DsfSha1MessageDigest(this_00);
    }
  }
  else {
    this_01 = (DsfSha256MessageDigest *)FUN_18002c9fc(0x10);
    if (this_01 == (DsfSha256MessageDigest *)0x0) {
      plVar2 = (longlong *)0x0;
    }
    else {
      *(undefined8 *)this_01 = 0;
      *(undefined8 *)(this_01 + 8) = 0;
      plVar2 = (longlong *)DsfSha256MessageDigest::DsfSha256MessageDigest(this_01);
    }
  }
  *(longlong **)(this + 0x20) = plVar2;
  (**(code **)(*plVar2 + 0x10))();
  *(undefined4 *)(this + 0x28) = 0;
  return;
}



/* ---- nativeSign @ 18002b180 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: int __cdecl DsfShaWithRsaSignature::nativeSign(unsigned char * __ptr64,int,int) __ptr64
    */

int __thiscall
DsfShaWithRsaSignature::nativeSign
          (DsfShaWithRsaSignature *this,uchar *param_1,int param_2,int param_3)

{
  int iVar1;
  int iVar2;
  int iVar3;
  undefined2 *_Dst;
  ulonglong uVar4;
  undefined8 uVar5;
  uint uVar6;
  uint uVar7;
  undefined1 auStackY_88 [32];
  longlong *local_58;
  longlong *local_50;
  void *local_48;
  ulonglong local_40;
  
                    /* 0x2b180  91  ?nativeSign@DsfShaWithRsaSignature@@QEAAHPEAEHH@Z */
  local_40 = DAT_180054f48 ^ (ulonglong)auStackY_88;
  local_58 = (longlong *)0x0;
  local_50 = (longlong *)0x0;
  if (*(int *)(this + 0x28) == 1) {
    iVar3 = *(int *)(this + 0x38);
    iVar1 = *(int *)(this + 0x3c);
    iVar2 = FUN_180009670(*(longlong **)(*(longlong *)(this + 0x10) + 0x10));
    uVar6 = iVar2 + 7U >> 3;
    if (((int)uVar6 <= param_3) && (iVar3 + iVar1 + 0xbU <= uVar6)) {
      _Dst = (undefined2 *)thunk_FUN_18002c9fc((ulonglong)uVar6);
      uVar5 = 0;
      memset(_Dst,0,(ulonglong)uVar6);
      *_Dst = 0x100;
      uVar7 = uVar6 - (iVar3 + iVar1);
      uVar4 = (ulonglong)uVar7;
      if (uVar7 != 0) {
        memset(_Dst + 1,(int)CONCAT71((int7)((ulonglong)uVar5 >> 8),0xff),(ulonglong)uVar7);
      }
      *(undefined1 *)((uVar4 - 1) + (longlong)_Dst) = 0;
      memcpy((void *)(uVar4 + (longlong)_Dst),*(void **)(this + 0x30),
             (ulonglong)*(uint *)(this + 0x38));
      iVar3 = (**(code **)(**(longlong **)(this + 0x20) + 0x48))
                        (*(longlong **)(this + 0x20),*(uint *)(this + 0x38) + uVar4 + (longlong)_Dst
                         ,*(undefined4 *)(this + 0x3c));
      if (iVar3 == *(int *)(this + 0x3c)) {
        iVar3 = FUN_1800177c0((longlong)_Dst,uVar6,(longlong *)&local_58);
        free(_Dst);
        if (iVar3 == 0) {
          iVar3 = thunk_FUN_18001e2b0(*(longlong *)(this + 0x10),local_58,(undefined *)0x0,0,
                                      &local_50,(longlong *)0x0);
          FUN_180009c50((longlong *)&local_58,(longlong *)0x0);
          if (iVar3 == 0) {
            uVar4 = FUN_180017750(local_50,uVar6,&local_48);
            FUN_180009c50((longlong *)&local_50,(longlong *)0x0);
            if ((int)uVar4 == 0) {
              memcpy(param_1 + param_2,local_48,(ulonglong)uVar6);
              FUN_180003aa0(local_48);
              nativeReset(this);
            }
          }
        }
        else {
          FUN_180009c50((longlong *)&local_58,(longlong *)0x0);
        }
      }
      else {
        free(_Dst);
      }
    }
  }
  iVar3 = FUN_18002c590(local_40 ^ (ulonglong)auStackY_88);
  return iVar3;
}



/* ---- nativeVerify @ 18002b390 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: int __cdecl DsfShaWithRsaSignature::nativeVerify(unsigned char * __ptr64,int,int) __ptr64
    */

int __thiscall
DsfShaWithRsaSignature::nativeVerify
          (DsfShaWithRsaSignature *this,uchar *param_1,int param_2,int param_3)

{
  int iVar1;
  undefined8 uVar2;
  uchar *_Dst;
  uchar *_Dst_00;
  uchar *puVar3;
  uchar *puVar4;
  uint *puVar5;
  ulonglong uVar6;
  undefined1 auStackY_78 [32];
  ulong local_48;
  uint local_44;
  ulonglong local_40;
  
                    /* 0x2b390  96  ?nativeVerify@DsfShaWithRsaSignature@@QEAAHPEAEHH@Z */
  local_40 = DAT_180054f48 ^ (ulonglong)auStackY_78;
  if ((*(int *)(this + 0x28) == 2) &&
     (uVar2 = FUN_18001f310(*(longlong *)(this + 0x10),&local_44), (int)uVar2 == 0)) {
    iVar1 = FUN_180009670(*(longlong **)(*(longlong *)(this + 0x10) + 0x10));
    uVar6 = (ulonglong)(iVar1 + 7U >> 3);
    _Dst = (uchar *)thunk_FUN_18002c9fc(uVar6);
    memset(_Dst,0xff,uVar6);
    memcpy(_Dst,*(void **)(this + 0x30),(ulonglong)*(uint *)(this + 0x38));
    iVar1 = (**(code **)(**(longlong **)(this + 0x20) + 0x48))
                      (*(longlong **)(this + 0x20),_Dst + *(uint *)(this + 0x38),
                       *(undefined4 *)(this + 0x3c));
    if (iVar1 == *(int *)(this + 0x3c)) {
      _Dst_00 = (uchar *)thunk_FUN_18002c9fc(uVar6);
      memset(_Dst_00,0xff,uVar6);
      puVar3 = param_1 + param_2;
      puVar5 = &local_48;
      puVar4 = _Dst_00;
      iVar1 = FUN_18001f6b0(*(longlong *)(this + 0x10),(longlong)puVar3,_Dst_00,puVar5,
                            (longlong *)0x0);
      if (iVar1 == 0) {
        iVar1 = DsfUtil::isDebugEnabled();
        if (iVar1 != 0) {
          FUN_180026f20("Comparing signatures:\n\n",puVar3,puVar4,puVar5);
          uVar6 = (ulonglong)(uint)(*(int *)(this + 0x3c) + *(int *)(this + 0x38));
          DsfObject::hexdump(_Dst,*(int *)(this + 0x3c) + *(int *)(this + 0x38));
          FUN_180026f20("\nwith:\n",uVar6,puVar4,puVar5);
          DsfObject::hexdump(_Dst_00,local_48);
        }
        memcmp(_Dst,_Dst_00,(ulonglong)*(uint *)(this + 0x3c));
        free(_Dst_00);
        free(_Dst);
        nativeReset(this);
      }
      else {
        free(_Dst);
        free(_Dst_00);
      }
    }
    else {
      free(_Dst);
    }
  }
  iVar1 = FUN_18002c590(local_40 ^ (ulonglong)auStackY_78);
  return iVar1;
}



/* ---- verify @ 18002ba00 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: unsigned char __cdecl DsfShaWithRsaSignature::verify(struct JNIEnv_ * __ptr64,class
   _jobject * __ptr64,class _jbyteArray * __ptr64) __ptr64 */

uchar __thiscall
DsfShaWithRsaSignature::verify
          (DsfShaWithRsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3)

{
  uchar uVar1;
  long lVar2;
  
                    /* 0x2ba00  128
                       ?verify@DsfShaWithRsaSignature@@QEAAEPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@@Z
                        */
  lVar2 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
  uVar1 = verify(this,param_1,param_2,param_3,0,lVar2);
  return uVar1;
}



/* ---- verify @ 18002ba70 ---- */

/* WARNING: Function: _guard_dispatch_icall replaced with injection: guard_dispatch_icall */
/* public: unsigned char __cdecl DsfShaWithRsaSignature::verify(struct JNIEnv_ * __ptr64,class
   _jobject * __ptr64,class _jbyteArray * __ptr64,long,long) __ptr64 */

uchar __thiscall
DsfShaWithRsaSignature::verify
          (DsfShaWithRsaSignature *this,JNIEnv_ *param_1,_jobject *param_2,_jbyteArray *param_3,
          long param_4,long param_5)

{
  int iVar1;
  uchar uVar2;
  int iVar3;
  enum_errDescrValues eVar4;
  int iVar5;
  longlong lVar6;
  undefined8 uVar7;
  ulonglong uVar8;
  undefined2 *_Buf1;
  uint uVar9;
  uint uVar10;
  undefined1 auStackY_98 [32];
  longlong *local_68;
  longlong *local_60;
  void *local_58;
  ulonglong local_50;
  
                    /* 0x2ba70  129
                       ?verify@DsfShaWithRsaSignature@@QEAAEPEAUJNIEnv_@@PEAV_jobject@@PEAV_jbyteArray@@JJ@Z
                        */
  local_50 = DAT_180054f48 ^ (ulonglong)auStackY_98;
  local_68 = (longlong *)0x0;
  if (*(int *)(this + 0x28) == 2) {
    iVar3 = FUN_180009670(*(longlong **)(*(longlong *)(this + 0x10) + 0x10));
    iVar5 = *(int *)(this + 0x38);
    iVar1 = *(int *)(this + 0x3c);
    uVar9 = iVar3 + 7U >> 3;
    iVar3 = (**(code **)(*(longlong *)param_1 + 0x558))(param_1,param_3);
    if ((param_5 == uVar9) && (param_5 + param_4 <= iVar3)) {
      lVar6 = (**(code **)(*(longlong *)param_1 + 0x6f0))(param_1,param_3,0);
      eVar4 = FUN_1800177c0(lVar6 + param_4,uVar9,(longlong *)&local_68);
      (**(code **)(*(longlong *)param_1 + 0x6f8))(param_1,param_3,lVar6);
      if (eVar4 == 0) {
        uVar7 = FUN_18001e7b0(*(longlong *)(this + 0x10),local_68,(longlong *)&local_60,
                              (longlong *)0x0);
        eVar4 = (enum_errDescrValues)uVar7;
        FUN_180009c50((longlong *)&local_68,(longlong *)0x0);
        if (eVar4 == 0) {
          uVar8 = FUN_180017750(local_60,uVar9,&local_58);
          uVar7 = 0;
          eVar4 = (enum_errDescrValues)uVar8;
          FUN_180009c50((longlong *)&local_60,(longlong *)0x0);
          if (eVar4 == 0) {
            _Buf1 = (undefined2 *)thunk_FUN_18002c9fc((ulonglong)uVar9);
            uVar10 = uVar9 - (iVar5 + iVar1);
            *_Buf1 = 0x100;
            if (0 < (int)uVar10) {
              memset(_Buf1 + 1,(int)CONCAT71((int7)((ulonglong)uVar7 >> 8),0xff),(ulonglong)uVar10);
            }
            *(undefined1 *)(((ulonglong)uVar10 - 1) + (longlong)_Buf1) = 0;
            memcpy((void *)((ulonglong)uVar10 + (longlong)_Buf1),*(void **)(this + 0x30),
                   (ulonglong)*(uint *)(this + 0x38));
            iVar5 = (**(code **)(**(longlong **)(this + 0x20) + 0x30))
                              (*(longlong **)(this + 0x20),param_1,param_2,
                               (ulonglong)*(uint *)(this + 0x38) + (ulonglong)uVar10 +
                               (longlong)_Buf1);
            if (iVar5 == *(int *)(this + 0x3c)) {
              memcmp(_Buf1,local_58,(ulonglong)uVar9);
              free(local_58);
              free(_Buf1);
              reset(this,param_1,param_2);
            }
            else {
              DsfObject::throwException
                        ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                         "error calculating final digest");
              free(local_58);
              free(_Buf1);
            }
            goto LAB_18002bbb3;
          }
        }
        DsfObject::throwException
                  ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                   "invalid signature",eVar4);
      }
      else {
        DsfObject::throwException
                  ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                   "error generating integer representative signature",eVar4);
        FUN_180009c50((longlong *)&local_68,(longlong *)0x0);
      }
    }
    else {
      DsfObject::throwException
                ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
                 "invalid signature");
    }
  }
  else {
    DsfObject::throwException
              ((DsfObject *)(this + 8),param_1,"java/security/SignatureException",
               "signature not initialized for verifying");
  }
LAB_18002bbb3:
  uVar2 = FUN_18002c590(local_50 ^ (ulonglong)auStackY_98);
  return uVar2;
}


