-- ============================================================
-- V2.07  涓氬姟鏍稿績琛?tenant_id 鍥炲～锛堝箓绛夛級
--
-- 鑳屾櫙锛?
--   V1.16 宸插 64 寮犱笟鍔¤〃缁熶竴 ALTER TABLE ... ADD COLUMN tenant_id
--   BIGINT NOT NULL DEFAULT 1锛屼絾瀛樺湪浠ヤ笅椋庨櫓锛?
--     1) V1.16 鍓嶆湰鍦板紑鍙戝簱鍙兘宸叉湁 tenant_id 鍒椾负 NULL 鐨勫巻鍙叉暟鎹?
--        锛堜緥濡傞€氳繃 ddl-auto=update 琛ュ垪銆佹垨涔嬪墠璺戣繃鍗婃垚鍝佽剼鏈悗鍥炴粴锛夛紱
--     2) 绉熸埛鏁版嵁琚鎸傚埌 tenant_id=0锛堝钩鍙颁繚鐣欏€硷級鐨勬瀬绔儏褰€?
--
--   鏈剼鏈宸ュ崟 / 娲惧伐 / 鐗╂枡 / 寮傚父鑱旂粶 4 寮犳牳蹇冧笟鍔¤〃鍋?鍏滃簳鍥炲～"锛?
--   灏?NULL 鍊肩粺涓€淇涓?1锛堥粯璁ょ鎴凤級锛屼繚璇佺敓浜у绉熸埛鏌ヨ鐨?tenant_id
--   杩囨护鏉′欢濮嬬粓鑳藉懡涓銆?
--
-- 骞傜瓑鎬э細
--   - WHERE tenant_id IS NULL锛氬湪 V1.16 宸插缓绔?NOT NULL 鐨勫垪涓?
--     鏈潯浠舵案杩滀负 false锛岄噸澶嶆墽琛?0 琛屽奖鍝嶏紝瀹屽叏骞傜瓑锛?
--   - 鏈潵濡傛灉鏌愬紶琛ㄨ璇敼涓?NULLABLE锛屾湰鑴氭湰鑷姩琛ユ晳銆?
--
-- 杩炶窇楠岃瘉锛?
--   $ mysql ... < V2.07__backfill_tenant_id.sql
--   $ mysql ... < V2.07__backfill_tenant_id.sql
--   涓ゆ鍧囧簲杩斿洖 SUCCESS锛岀浜屾鐨?Rows matched: 0 / Changed: 0銆?
--
-- 鑼冨洿璇存槑锛?
--   鏈鍙鐩栦换鍔℃弿杩颁腑鐨?4 寮?鏍稿績"涓氬姟琛紱鍏朵綑 60 寮犱笟鍔¤〃鐨?
--   鍏滃簳鍥炲～鐣欑粰鍚庣画涓撻」鑴氭湰锛堟垨鐢?DBA 鎸夐渶 ad-hoc 鎵ц锛夛紝浠ュ厤
--   Flyway 鍗曚簨鍔¤繃澶с€侀攣琛ㄦ椂闂磋繃闀裤€?
--
-- 渚濊禆锛歏1.16銆乂1.17 宸叉墽琛屻€?
-- ============================================================

-- 1) 鐢熶骇宸ュ崟锛坢es_work_order锛?----------------------------------------
UPDATE mes_work_order
   SET tenant_id = 1
 WHERE tenant_id IS NULL;

-- 2) 娲惧伐浠诲姟锛坢es_dispatch_task锛?-------------------------------------
UPDATE mes_dispatch_task
   SET tenant_id = 1
 WHERE tenant_id IS NULL;

-- 3) 鐗╂枡妗ｆ锛坢es_material锛?------------------------------------------
UPDATE mes_material
   SET tenant_id = 1
 WHERE tenant_id IS NULL;

-- 4) 寮傚父鑱旂粶鍗曪紙mes_abnormal_contact锛?--------------------------------
UPDATE mes_abnormal_contact
   SET tenant_id = 1
 WHERE tenant_id IS NULL;
