-- ============================================================================
-- V1.16 ALTER 鎷嗗垎鑽夋锛圴1.16 Split Proposal锛?
--
-- 鏈枃浠朵负 **鏂规鑽夋**锛屼笉鍏?sql/migration/ 姝ｅ紡鐩綍锛屼笉浼氳 Flyway 鎷惧彇銆?
-- 鐩殑锛氭妸鍘?V1.16__add_tenant_id_to_all_business_tables.sql 涓?64 鏉?ALTER +
--      12 鏉?CREATE INDEX锛堝叡 76 鏉?DDL锛夋媶鍒嗕负 10 涓瓙 migration锛岄檷浣庡崟涓?
--      migration 鐨?wall time锛岃閬夸互涓嬮闄╋細
--   1. titan-mysql 绛夊叡浜疄渚嬪湪涓氬姟楂樺嘲鎵ц ALTER 鏃跺嚭鐜?`Lost connection`
--      锛坢cp9 v1 鍦?V1.16 绗?33 鏉?ALTER 閬囧埌杩囪閿欒锛夛紱
--   2. Flyway 榛樿 `lockRetryCount=50`锛屽崟涓?migration 瓒呮椂浼氳鍏朵粬鑺傜偣闀挎椂闂?
--      绛夊緟 flyway_schema_history 鐨勮閿侊紝褰卞搷澶氬疄渚嬫粴鍔ㄦ洿鏂帮紱
--   3. 渚夸簬鍥炴粴锛氬鏌愬紶琛ㄧ殑 ADD COLUMN 闇€瑕佹挙閿€锛屽彧闇€鍥為€€瀵瑰簲瀛?migration銆?
--
-- 鎷嗗垎绛栫暐锛?
--   - 鎸変笟鍔℃ā鍧楋紙琛ㄥ悕鍓嶇紑锛夊垎缁勶紝姣忕粍 鈮?10 涓〃锛屼娇鐢ㄧ嫭绔嬬増鏈彿锛?
--   - 鎵€鏈夊瓙 migration 鍏辩敤 DEFAULT 1锛堟部鐢ㄥ師鑴氭湰璇箟锛夛紱
--   - 绱㈠紩鍒涘缓鏀惧埌鏈€鍚庝竴涓瓙 migration锛圴1.16.10锛夛紝閬垮厤 ALTER 涓?CREATE INDEX
--     浜ゅ弶寮曞彂鍏冩暟鎹攣绔炰簤锛?
--   - 鎵€鏈?ADD COLUMN 浣跨敤 `IF NOT EXISTS`锛圡ySQL 8.0.29+ 鏀寔锛夌‘淇濆箓绛夐噸璺戯紱
--   - 濡傛灉鍥㈤槦 MySQL 鐗堟湰 < 8.0.29锛岃鎶?`IF NOT EXISTS` 鍘绘帀骞跺湪 Flyway
--     `repair` 鍚庡啀璺戙€?
--
-- 浣滆€咃細mcp9 (v2)  鏃ユ湡锛?026-04-22
-- 鍏宠仈浠诲姟锛歮es_m8_sql_migration_mcp9_v2
-- ============================================================================

-- ----------------------------------------------------------------------------
-- V1.16.01__add_tenant_id_work_order_tables.sql  锛坵ork_order 瀹舵棌 15 寮狅級
-- 棰勪及鑰楁椂锛氬崟 ALTER ~0.5s锛堢┖琛級/ 绾夸笂鍏ㄨ〃 ~30s锛涘叏缁?wall time 鈮?10 鍒嗛挓
-- ----------------------------------------------------------------------------
ALTER TABLE mes_work_center                    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_instruction               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_instruction_person        ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order                     ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_attachment          ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_constraint          ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_input_material      ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_output_material     ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_quality_item        ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_status_log          ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_supply_plan         ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_task                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_order_task_segment        ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_start_check               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_work_status_view               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.02__add_tenant_id_material_tables.sql  锛坢aterial 瀹舵棌 5 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_material                       ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_material_price                 ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_material_requisition           ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_material_requisition_item      ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_material_return                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.03__add_tenant_id_instruction_tables.sql  锛坕nstruction 4 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_instruction                    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_instruction_flow_log           ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_instruction_serial             ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_instruction_stage              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.04__add_tenant_id_aps_tables.sql  锛圓PS 闆嗘垚 4 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_aps_data_mapping               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_aps_sync_detail                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_aps_sync_log                   ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_aps_sync_queue                 ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.05__add_tenant_id_production_recheck_abnormal.sql
-- 锛坧roduction 3 寮?+ recheck 3 寮?+ abnormal 3 寮?= 9 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_production_plan                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_production_team                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_production_work                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_recheck_order_plan             ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_recheck_request                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_recheck_serial                 ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_abnormal_contact               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_abnormal_contact_attachment    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_abnormal_contact_log           ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.06__add_tenant_id_finished_dispatch.sql
-- 锛坒inished_goods_receipt* 3 寮?+ dispatch* 3 寮?= 6 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_finished_goods_receipt         ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_finished_goods_receipt_item    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_finished_goods_receipt_request ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_dispatch_assignment            ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_dispatch_status_log            ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_dispatch_task                  ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.07__add_tenant_id_bom_process_resource.sql
-- 锛坆om* 2 寮?+ process* 2 寮?+ resource* 2 寮?+ shift* 2 寮?+ manufacturing* 2 寮?= 10 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_bom_substitute                 ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_bom_version_log                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_process_info                   ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_process_template               ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_resource_calendar              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_resource_calendar_shift        ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_shift_handover                 ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_shift_handover_attachment      ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_manufacturing_bom              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_manufacturing_bom_item         ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.08__add_tenant_id_order_misc_1.sql  锛堝墿浣?order* 2 寮?+ 鏉傞」 5 寮?= 7 寮狅級
-- ----------------------------------------------------------------------------
ALTER TABLE mes_order_plan                     ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_order_start_check              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_transfer_order                 ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_inspection_work                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_storage_inventory              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_spray_condition                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_outsource_order                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.09__add_tenant_id_misc_2.sql  锛堝墿浣?4 寮犲崟浣撹〃锛?
-- ----------------------------------------------------------------------------
ALTER TABLE mes_machining_program              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_plan_status_log                ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_requisition_order              ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';
ALTER TABLE mes_delivery_sign                  ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '绉熸埛ID';

-- ----------------------------------------------------------------------------
-- V1.16.10__add_tenant_id_indexes.sql  锛堢粺涓€寤虹储寮?12 鏉★紝鏀炬渶鍚庝竴缁勶級
-- 閲嶈锛氱储寮曞缓璁湪鎵€鏈?ADD COLUMN 瀹屾垚鍚庡啀寤猴紝鍚﹀垯涓棿鐘舵€佷細瑙﹀彂绱㈠紩閲嶅缓
-- 濡備笟鍔¤〃鏁版嵁閲?> 1M 琛岋紝寤鸿鍦ㄤ笟鍔′綆宄扮獥鍙ｅ崟鐙窇鏈枃浠?
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_tenant_material          ON mes_material(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_work_center       ON mes_work_center(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_work_order        ON mes_work_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_order_plan        ON mes_order_plan(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_dispatch_task     ON mes_dispatch_task(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_process_info      ON mes_process_info(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_production_team   ON mes_production_team(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_manufacturing_bom ON mes_manufacturing_bom(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_quality           ON mes_inspection_work(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_material_req      ON mes_material_requisition(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_abnormal          ON mes_abnormal_contact(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_shift             ON mes_shift_handover(tenant_id);

-- ============================================================================
-- 钀藉湴鏀归€犳寚鍗楋紙Migration Plan锛?
--
-- 1. 鏈崏妗堜笉瑕佺洿鎺?copy 鍒?sql/migration/ 鐩綍锛氳繖鏍蜂細涓?flyway_schema_history 涓?
--    鐜版湁鐨?V1.16 璁板綍鍐茬獊锛坈hecksum 涓嶅尮閰?鈫?鍚姩澶辫触锛夈€?
--
-- 2. 鏀归€犺矾寰勶紙闆跺仠鏈猴級锛?
--    a. 鏂板垎鏀?feat/sql-v1.16-split
--    b. 鍦?sql/migration/ 寤虹珛 V1.16.01 ~ V1.16.10锛團lyway 浼氭寜 1.16.01 > 1.16
--       瀛楀吀搴忚瘑鍒负鏂?migration锛夛紱
--    c. 淇濈暀鍘?V1.16 涓嶅姩锛堝凡涓婄嚎鐜宸叉湁 SUCCESS 璁板綍锛屼笉鑳芥敼鍚嶄笉鑳芥敼 checksum锛夛紱
--    d. 鐢变簬鎵€鏈夋柊 migration 鐢?`IF NOT EXISTS` 淇濇姢锛?*瀵瑰凡缁忚窇杩?V1.16 鐨勮€佺幆澧?
--       鏄?no-op**锛屽彧鏄湪 flyway_schema_history 閲屽姞 10 鏉℃柊鐨?SUCCESS锛?
--    e. 瀵?*鍏ㄦ柊鐜**璺?V1.16 + V1.16.01~10锛岀粨鏋滅瓑浠凤紝涓?wall time 鏇村弸濂姐€?
--
-- 3. 鍏煎鎬э細
--    - MySQL 8.0.29+ 鍘熺敓鏀寔 `IF NOT EXISTS` 瀛愬彞锛?
--    - MySQL 8.0.29 浠ヤ笅鐗堟湰璇峰幓鎺?IF NOT EXISTS 骞舵墜宸ヤ繚璇佷笉閲嶅鎵ц锛?
--    - 娴嬭瘯鐜 titan-mysql 杩愯 8.0.45锛屽凡鏀寔锛?
--    - 鐢熶骇 MySQL 鐗堟湰浠?DBA 浜や粯娓呭崟涓哄噯锛屾湰鑽夋鍦ㄤ笂绾垮墠蹇呴』 DBA 绛惧瓧纭銆?
--
-- 4. 鍥炴粴璁″垝锛?
--    - 姣忎釜瀛?migration 瀵瑰簲涓€涓洖婊氳剼鏈紙鍛藉悕 V1.16.XX__rollback.sql锛?
--    - 鍥炴粴鍐呭 = `ALTER TABLE xxx DROP COLUMN IF EXISTS tenant_id;`
--    - 鐢变簬褰撳墠鏁翠釜椤圭洰鐨?RBAC/瀹¤閮介噸搴︿緷璧?tenant_id锛屽疄闄呬笂**涓嶅簲鍥炴粴 V1.16**锛?
--      鏈潯浠呬负绱ф€ユ晠闅滅獥鍙ｇ殑淇濆懡寮€鍏筹紝涓嶄綔甯歌浣跨敤銆?
--
-- 5. 鎷嗗垎鍚庨浼版敼鍠勶細
--    - 鍗?migration 鏈€澶?DDL 鏁伴噺锛?5锛堝師 V1.16 = 76锛?
--    - 鍗?migration 棰勬湡 wall time锛氣墹 2min锛堝師 V1.16 = 10~15min锛?
--    - Flyway lock 鎸佹湁鏃堕棿锛氫粠鍘熸潵鐨?"鍏ㄩ儴 76 鏉?DDL" 闄嶄负 "鍒嗙粍鍐?15 鏉?
--    - 澶辫触閲嶈瘯绮掑害锛氫粠"鏁翠釜 V1.16 閲嶈窇"闄嶄负"鍙噸璺戝け璐ョ殑瀛?group"
--
-- ============================================================================
-- EOF  V1.16_split_proposal.sql
