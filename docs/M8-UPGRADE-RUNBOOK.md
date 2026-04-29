# M8 涓婄嚎鍗囩骇鍓ф湰锛圧unbook锛?

| 椤?| 鍐呭 |
| --- | --- |
| 鍓ф湰缂栧彿 | `M8-UPGRADE-RUNBOOK` |
| 閫傜敤鑼冨洿 | M7 鈫?M8 鐢熶骇鐜鍗囩骇锛堝崟浣?/ HA / 寰湇鍔′笁绉嶅舰鎬佸潎閫傜敤锛岀ず渚嬩互 HA 涓轰富锛?|
| 鐩爣璇昏€?| 杩愮淮 / DevOps锛堝彲浠ユ槸**娌＄杩囨湰椤圭洰**鐨勬柊鍚屽锛?|
| 鎵ц绐楀彛 | 寤鸿鍛ㄦ湯 01:00 ~ 05:00锛?h 绐楀彛锛屽惈鍥炴粴锛?|
| 鍥炴粴绐楀彛 | 0:00 ~ 0:30 濡傚彂鐜伴樆濉炲嵆鍙?`docker compose down` 鍥炴粴 |
| 鍗囩骇鎴愬姛鎸囨爣 | `/api/actuator/health` UP + 5 鏉￠摼璺啋鐑熷叏缁?+ AlertManager 鏃犳柊鍛婅鎸佺画 30 min |
| 鍥炴粴 RTO | 鈮?30 min锛堝熀浜庡揩鐓?+ docker tag 鍒囨崲锛?|
| 璇佹嵁鏃ュ織 | `/var/log/mes-upgrade/YYYYMMDD/`锛堟瘡姝ュ繀鐣欙級 |

> 鏈?Runbook 閰嶅銆奙8 缁堥獙鎬绘姤鍛娿€媊docs/M8-FINAL-ACCEPTANCE-REPORT.md` 鐨勪笂绾?Go/No-Go Checklist锛涢厤濂椼€婅繍缁存晠闅滄墜鍐屻€媊docs/operations/runbook.md`锛涢厤濂椼€婂浠芥仮澶?SOP銆媊docs/operations/backup-restore.md`銆傚嚭鐜颁换浣曟湭瑕嗙洊鍦烘櫙锛屼紭鍏堟寜杩欎笁浠芥枃妗ｅ鐞嗐€?

---

## 鐩綍

1. [鍓嶇疆妫€鏌ワ紙T-24h锛塢(#涓€鍓嶇疆妫€鏌-24h)
2. [鍋滄満绐楀彛鎿嶄綔搴忓垪锛圱0 鈫?T+90min锛塢(#浜屽仠鏈虹獥鍙ｆ搷浣滃簭鍒梩0--t90min)
3. [鍋ュ悍妫€鏌ヤ笌鍐掔儫锛圱+90 鈫?T+120min锛塢(#涓夊仴搴锋鏌ヤ笌鍐掔儫t90--t120min)
4. [鍥炴粴 SOP](#鍥涘洖婊?sop)
5. [甯歌鍗囩骇鏁呴殰 Troubleshoot](#浜斿父瑙佸崌绾ф晠闅?troubleshoot)
6. [鍗囩骇鍚?24h 瑙傚療娓呭崟](#鍏崌绾у悗-24h-瑙傚療娓呭崟)
7. [闄勫綍 A 路 鐜鍙橀噺娓呭崟锛圡8 鍏ㄩ噺锛塢(#闄勫綍-a--鐜鍙橀噺娓呭崟m8-鍏ㄩ噺)
8. [闄勫綍 B 路 浜哄憳涓庢矡閫氱煩闃礭(#闄勫綍-b--浜哄憳涓庢矡閫氱煩闃?

---

## 涓€銆佸墠缃鏌ワ紙T-24h锛?

> 鍗囩骇绐楀彛鍓?*涓€澶?*瀹屾垚銆備换浣曚竴椤规湭杈炬爣閮戒笉瑕佽繘鍏?搂浜岋紱缁х画杩愯鑰佺増鏈洿鍒拌ˉ榻愩€?

### 1.1 纭欢 / 瀹夸富璧勬簮

| 椤?| 闃堝€?| 鏍￠獙鍛戒护锛圠inux锛?| 鏍￠獙鍛戒护锛圵indows锛?|
| --- | --- | --- | --- |
| 纾佺洏鍓╀綑绌洪棿锛坄/var/lib/docker` + `/backup`锛?| 鈮?100 GB | `df -h /var/lib/docker /backup` | `Get-PSDrive C \| Select Used,Free` |
| 鍐呭瓨鍓╀綑 | 鈮?8 GB | `free -g` | `Get-Counter '\Memory\Available MBytes'` |
| 绔彛鍗犵敤锛?0 / 443 / 9090 / 3306 / 3307 / 6379 / 9000 / 9001 / 5672 / 15672 / 8848 / 8849 / 8850 / 9091 / 9093 / 3000锛?| 鍧囩┖闂诧紙鎴栧凡鐢辨湰娆?MES 鍗犵敤锛?| `ss -tulpn \| grep -E ':(80\|443\|9090\|3306...)'` | `netstat -ano \| Select-String ':9000 '` |
| Docker 鐗堟湰 | 鈮?24.0.0锛坈ompose v2 鍘熺敓锛?| `docker --version && docker compose version` | 鍚?|
| Docker daemon 鍙揪 | `docker info` exit 0 | `docker info` | 鍚?|

### 1.2 浠ｇ爜 / 鍒跺搧榻愬

| 椤?| 鍛戒护 / 浣嶇疆 |
| --- | --- |
| 鍗囩骇鍒嗘敮 tag 宸插垱寤?| `git tag v1.1.0 && git push origin v1.1.0` |
| CI 鐨?`docker-build-push` job 宸蹭负 tag 鎵撻暅鍍?| 妫€鏌?GHCR `ghcr.io/mes/mes-admin:v1.1.0` 鍙?`docker pull` |
| 鍓嶇 dist 宸叉帹閫佸埌 CDN锛堝鏈夛級 | `ls dist/` / 鎴栫洿鎺ヤ娇鐢ㄩ暅鍍?`ghcr.io/mes/mes-frontend:v1.1.0` |
| 鏃х増鏈暅鍍忎粛淇濈暀锛堢敤浜庡洖婊氾級 | `docker images \| grep mes- \| grep v1.0.` |

### 1.3 鐜鍙橀噺涓庡瘑閽?

**P0 鏍￠獙**鈥斺€斾互涓?5 鏉″繀椤婚綈澶囷紝缂轰竴 `ProdEnvValidator` 浼?fail-fast锛?

```bash
# MES_JWT_SECRET: >= 32 瀛楄妭闅忔満
echo $MES_JWT_SECRET | wc -c    # 鏈熸湜 >= 33锛堝惈鎹㈣锛?

# SPRING_DATASOURCE_PASSWORD: 闈炵┖ / 闈炲急鍊?
echo $SPRING_DATASOURCE_PASSWORD | wc -c

# SPRING_RABBITMQ_PASSWORD: 闈炵┖ / 闈炲急鍊?
# MES_APS_API_KEY: 闀垮害 >= 16 涓斾笉鍛戒腑 mes-default-api-key / default / test / 123456
# MES_APS_HMAC_KEY: 闀垮害 >= 32
```

鍏跺畠锛堟帹鑽?KMS 娉ㄥ叆锛夛細

```
MYSQL_ROOT_PASSWORD
MYSQL_REPLICATION_PASSWORD
REDIS_PASSWORD
RABBITMQ_ERLANG_COOKIE
RABBITMQ_PASSWORD
NACOS_AUTH_TOKEN
NACOS_PASSWORD
MINIO_ROOT_USER
MINIO_ROOT_PASSWORD
MES_FILE_STORAGE_TYPE=minio
MES_FILE_MINIO_ENDPOINT=http://minio:9000
MES_FILE_MINIO_BUCKET=mes
MES_FILE_MINIO_ACCESS_KEY=<mesadmin>
MES_FILE_MINIO_SECRET_KEY=<寮洪殢鏈?
GRAFANA_PASSWORD
DINGTALK_ROBOT_P0_URL=https://oapi.dingtalk.com/robot/send?access_token=...
DINGTALK_ROBOT_P1_URL=https://oapi.dingtalk.com/robot/send?access_token=...
MES_ENV=prod
```

### 1.4 DB 澶囦唤锛堜笉鍋氫笉鍗囩骇锛?

```bash
cd /opt/mes
bash scripts/backup/mysql-backup.sh --mode=full 2>&1 | tee \
  /var/log/mes-upgrade/$(date +%Y%m%d)/pre-upgrade-full-backup.log
```

纭锛?

1. 鏈湴 `/backup/mysql/daily/mes_daily_YYYYMMDD_*.sql.gz.gpg` 鐢熸垚
2. MinIO `mes-backups/daily/` 鍚屾瀹屾垚锛坄mc ls local/mes-backups/daily/ | tail -5`锛?
3. `bash scripts/backup/verify-backup.sh` 瀹¤璁板綍鐢熸垚锛堟娊鏍锋仮澶?+ 琛屾暟鏍″锛?

### 1.5 鍛婅闈欓粯 / 缁存姢绐楀彛

> 鈿狅笍 **P0-1锛堝墠缃級**锛歚docker-compose.ha.yml` 涓?*娌℃湁** alertmanager/prometheus 瀹瑰櫒锛屽畠浠敱 `docker-compose.monitoring.yml` 鐙珛鎻愪緵銆傚洜姝ゆ湰鑺傛墍鏈?`http://localhost:9093` / `http://localhost:9091` 鍛戒护鐨?*闅愬惈鍓嶆彁**鏄洃鎺ф爤宸茬粡鍜?HA 鏍堝彔鍔犲惎鍔細
>
> ```bash
> docker compose -f docker-compose.ha.yml -f docker-compose.monitoring.yml up -d \
>   mes-prometheus mes-alertmanager mes-grafana
> # 绛夊緟 30 绉掕 alertmanager 鍔犺浇閰嶇疆
> until curl -fsS http://localhost:9093/-/healthy; do sleep 3; done
> ```
>
> 杩欏悓鏍烽€傜敤浜?搂2.14锛堣В闄ら潤榛橈級銆伮?.7锛圥rometheus 鎸囨爣鏌ヨ锛夈€伮?锛?4h 瑙傚療锛夐噷鎵€鏈?9091/9093 璋冪敤銆?*鍙捣 HA compose 涓嶈捣 monitoring compose 浼氳鏈妭闈欓粯鍛戒护鐩存帴 connection refused**锛孌evOps 浼氳浠ヤ负鍛婅宸查潤榛樿€屽疄闄呮病鏈夈€?

```bash
# 闈欓粯 AlertManager 2 灏忔椂
curl -X POST http://localhost:9093/api/v2/silences \
  -H 'Content-Type: application/json' \
  -d '{
    "matchers":[{"name":"severity","value":"P[012]","isRegex":true}],
    "startsAt":"2026-04-22T01:00:00+08:00",
    "endsAt":"2026-04-22T05:00:00+08:00",
    "createdBy":"upgrade-m8",
    "comment":"M8 upgrade window; auto-expire 2026-04-22 05:00"
  }'
```

鈿狅笍 **涓嶈**闈欓粯杩囬暱锛涗弗绂侀潤榛樺叏閮ㄥ憡璀︺€備笂绾垮悗绔嬪嵆瑙ｉ櫎锛屾敼鐢变汉宸ヨ瀵熴€?

> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-1 淇**锛氳ˉ鍓嶇疆鍚姩鐩戞帶鏍堝彔鍔犲懡浠わ紝鐐瑰悕浜?搂2.14 / 搂3.7 / 搂6 涔熼€傜敤銆?

### 1.6 Go / No-Go 鍐宠

濉啓骞舵妱閫佽繍缁?Lead + 涓氬姟 Lead锛?

- [ ] 纭欢璧勬簮妫€鏌ュ叏閮ㄩ€氳繃
- [ ] 鍒跺搧 / 闀滃儚榻愬
- [ ] 鐜鍙橀噺 5 P0 鏍￠獙閫氳繃
- [ ] DB 澶囦唤 + 鏍￠獙閫氳繃
- [ ] 鍛婅闈欓粯 + 缁存姢绐楀彛鍏憡鍙戦€?
- [ ] 涓氬姟渚у凡閫氱煡鍋滄満绐楀彛 + 鍥炴粴棰勬
- [ ] 鍥炴粴鑴氭湰鍙敤 + 鍓嶄竴鐗堟湰闀滃儚鍙?pull
- [ ] **锛圥0-1 鏂板锛?* 鐩戞帶鏍?`monitoring.yml` 宸插彔鍔犲惎鍔紝`mes-prometheus` / `mes-alertmanager` 瀹瑰櫒鍧?healthy 鈥斺€?`curl -fsS http://localhost:9093/-/healthy && curl -fsS http://localhost:9091/-/ready` 鍧囪繑鍥?200
- [ ] **锛圥1-6 鏂板锛?* `.env` 宸插寘鍚?`APS_BASE_URL` / `APS_OUTBOUND_API_KEY` / `MES_FILE_MINIO_ACCESS_KEY` / `MES_FILE_MINIO_SECRET_KEY` / `DINGTALK_ROBOT_P0_URL` / `DINGTALK_ROBOT_P1_URL` / `MES_ENV` / `BACKUP_GPG_PASSPHRASE`锛堝彲鐩存帴澶嶅埗鑷?`.env.example`锛岃闄勫綍 A 鑴氭敞锛夛紱涓?`MES_FILE_STORAGE_TYPE=minio`
- [ ] **锛圥0-4 鏂板锛?* `docker exec mes-frontend test -f /etc/nginx/conf.d/nginx.maintenance.conf` 杩斿洖 0锛屽苟涓?`nginx -t` 閫氳繃 鈥斺€?缁存姢椤甸缃埌浣嶏紙闀滃儚宸?COPY 涓や唤 conf锛?

---

## 浜屻€佸仠鏈虹獥鍙ｆ搷浣滃簭鍒楋紙T0 鈫?T+90min锛?

> 浠?HA compose 涓轰緥锛涘崟浣?/ 寰湇鍔″湪鍛戒护灞傛槸涓€鑷寸殑锛宑ompose 鏂囦欢鍚嶄笉鍚屻€?
> **姣忎竴姝ュ繀鐣?screenshot / log 鍒?`/var/log/mes-upgrade/YYYYMMDD/`**銆?

### 2.1 T0 路 鍐荤粨涓氬姟娴侀噺锛?min锛?

> 鈿狅笍 **P0-4 淇**锛氳€佺増鏈?Runbook 鍐欑殑 `nginx -s reload -c /etc/nginx/nginx.maintenance.conf` 鍦ㄥ綋鍓?`mes-frontend` 闀滃儚閲?*涓嶅彲鐢?*銆傚師鍥狅細nginx `-s reload -c` 鍙帴鍙楀畬鏁寸殑 **涓婚厤缃?*鏂囦欢锛堝惈 events/http 绛夊潡锛夛紝鑰屾垜浠殑缁存姢椤垫槸 `server {}` 绾?include 鐗囨銆傛纭仛娉曟槸 `cp` 瑕嗙洊 `default.conf` 鍚庡啀 `-s reload`锛堥暅鍍忓凡棰勭疆 `nginx.ha.conf` 鍜?`nginx.maintenance.conf` 鍒?`/etc/nginx/conf.d/`锛夈€?

```bash
# 鍓嶇锛氭妸 nginx 鍒囨崲鍒?maintenance 椤碉紙闀滃儚鍐呯疆 /etc/nginx/conf.d/nginx.maintenance.conf锛?
docker exec mes-frontend sh -c '
  cp /etc/nginx/conf.d/nginx.maintenance.conf /etc/nginx/conf.d/default.conf &&
  nginx -t &&
  nginx -s reload
'
# 棰勬湡锛氫换浣?GET / 杩斿洖 503 + HTML 缁存姢椤碉紱浠讳綍 POST|GET /api/ 杩斿洖 503 JSON + Retry-After: 5400

# 鏍￠獙宸插垏鍒扮淮鎶ら〉锛堝搷搴斾綋涓惈"MES 姝ｅ湪缁存姢鍗囩骇"锛?
curl -sS -o /dev/null -w '%{http_code}\n' http://localhost/          # 鏈熸湜 503
curl -sS -o /dev/null -w '%{http_code}\n' http://localhost/api/any   # 鏈熸湜 503
curl -sS http://localhost/ | grep -q "MES 姝ｅ湪缁存姢鍗囩骇" && echo "[OK] 缁存姢椤电敓鏁?

# 鎴栫畝鍗曠矖鏆达細鎶?nginx 瀹瑰櫒鍋滄帀锛岃 LB 杩斿洖 502 瑙﹀彂 SLB 鍋ュ悍妫€鏌?
# docker compose -f docker-compose.ha.yml stop mes-frontend
```

> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-4 淇**锛氭敼鎺?`nginx -s reload -c <file>` 杩欑鏃犳晥鍐欐硶锛屾敼涓?`cp + reload` 鍘熷湴鍒?conf锛涙柊澧炴牎楠屽懡浠ら槻姝?璇互涓虹敓鏁堜絾瀹為檯娌″垏"銆傞厤濂楁敼鍔細`mes-frontend/nginx.maintenance.conf` 鏂板锛沗mes-frontend/Dockerfile` 澧炲姞涓よ `COPY nginx.ha.conf` / `COPY nginx.maintenance.conf` 鍒?`/etc/nginx/conf.d/`銆?

### 2.2 T+2min 路 鍏抽棴瀹氭椂浠诲姟 + 寮傛娑堣垂

```bash
# 1) 绂佺敤 XXL-Job 鎵ц鍣紙prod-admin 闈㈡澘 鈫?鎵ц鍣?鈫?绂佺敤锛?
# 鎴栧簲鐢ㄤ晶寮€鍏筹細
docker exec mes-backend-1 curl -s -X POST \
  "http://localhost:9090/actuator/shutdownJobs?enable=false"

# 2) 璁?RabbitMQ 娑堣垂鑰呬紭闆呭仠锛歮es-backend 瀹瑰櫒鍐?kill -HUP 1
docker exec mes-backend-1 kill -HUP 1
docker exec mes-backend-2 kill -HUP 1
sleep 10
```

### 2.3 T+5min 路 DB 澶囦唤锛堝啀涓€娆★級+ binlog 浣嶇偣鐣欑棔

```bash
bash scripts/backup/mysql-backup.sh --mode=full 2>&1 | tee \
  /var/log/mes-upgrade/$(date +%Y%m%d)/T5-full-backup.log

# 璁板綍褰撳墠 binlog 浣嶇偣锛岀敤浜庝竾涓€鍥炴粴鐨?PITR
docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "SHOW MASTER STATUS\G" > /var/log/mes-upgrade/$(date +%Y%m%d)/T5-binlog.log
cat /var/log/mes-upgrade/$(date +%Y%m%d)/T5-binlog.log
# 鏈熸湜鐪嬪埌 File / Position / Executed_Gtid_Set
```

### 2.4 T+12min 路 鍋?Backend / Gateway锛堜繚鐣?DB / Redis / MQ / Nacos 杩愯锛?

```bash
docker compose -f docker-compose.ha.yml stop \
  mes-gateway-1 mes-gateway-2 \
  mes-backend-1 mes-backend-2
```

**娉ㄦ剰**锛歁ySQL / Redis / RabbitMQ / Nacos / MinIO 缁х画杩愯锛屽洜 Flyway 瑕佸湪 backend 閲嶅惎鏃惰嚜鍔ㄦ墽琛?`V1.16 ~ V2.05` 鐨勬柊杩佺Щ锛屼腑闂翠欢蹇呴』鍦ㄧ嚎銆?

### 2.5 T+15min 路 Flyway schema 鑷锛堝彲閫変絾寮虹儓鎺ㄨ崘锛?

```bash
docker run --rm --network=mes_mes-net \
  -e FLYWAY_URL=jdbc:mysql://mysql-primary:3306/mes \
  -e FLYWAY_USER=mes_app \
  -e FLYWAY_PASSWORD="$MES_APP_PASSWORD" \
  -v $(pwd)/sql:/flyway/sql:ro \
  flyway/flyway:10-alpine info
```

鏈熸湜杈撳嚭锛氭墍鏈?`V1.00 ~ V2.04 + V99_99` 涓?SUCCESS锛沗V2.05 / V1.18 / V1.19 / V1.20` 涓?**Pending**銆?

濡傛灉杈撳嚭寮傚父锛堟瘮濡?V1.16 鍥犱负鍘嗗彶鍘熷洜娌℃墽琛岋級锛?*绔嬪嵆涓骞跺悜棰戦亾 7 / mcp9 姹傚姪**銆?

### 2.6 T+20min 路 鍑嗗鏂扮増鏈暅鍍?

> 鈿狅笍 **P0-3 淇**锛氬綋鍓?`docker-compose.ha.yml` 姣忎釜 service 閮芥槸 `build.context: ./mes-backend`锛坆uild-from-source 妯″紡锛夈€?*娌℃湁 `image: ghcr.io/mes/...:${MES_VERSION}` 瀛楁**锛涙晠鍘?Runbook 鐨?`docker pull ghcr.io/mes/...` 鍛戒护**鎷変笅鏉ョ殑闀滃儚涓嶄細琚?compose 浣跨敤**锛岀櫧璐规祦閲忋€傞€夋嫨浠ヤ笅 **鏂规 A**锛氳 compose 鍘熷湴 rebuild 鈥斺€?鏃繚鎸佷笌 ha.yml 鐜扮姸涓€鑷达紝涔熶笉寮哄埗寮曞叆 Registry銆?
>
> 鑻ユ湭鏉?ha.yml 鏀逛负 `image:` 寮曠敤 Registry锛屽啀鍚屾鍒囧洖 pull 娴佺▼锛堟柟妗?B锛夊苟鎭㈠ 搂2.7 鐨?`sed MES_VERSION`銆?

```bash
# 鏂规 A锛氫粠褰撳墠 checkout 鐨勬簮鐮佸拰 Dockerfile 閲嶅缓鍏ㄩ儴 MES 闀滃儚锛堟帹鑽愩€佸綋鍓嶇幇鐘跺敮涓€鍙敤锛?
# 鍙寚瀹?--pull 璁?base image锛坢aven:3.9 / eclipse-temurin:17-jre-alpine / nginx:1.25-alpine-slim锛変篃鏇存柊
docker compose -f docker-compose.ha.yml build --pull --parallel 2>&1 | tee \
  /var/log/mes-upgrade/$(date +%Y%m%d)/T20-image-build.log

# 鏌ョ湅鏂伴暅鍍忔槸鍚︾敓鎴?
docker images | grep -E '^(mes-|mes_)' | head
# 棰勬湡鐪嬪埌锛歮es-mes-admin / mes-mes-gateway / mes-mes-*-service 绛夎嚦灏?9 鏉¤褰曪紝
# CREATED 鍦?5 鍒嗛挓鍐咃紙鍐?build 鍙兘 20+ 鍒嗛挓锛屽彈 mvn 渚濊禆涓嬭浇鍜?npm ci 褰卞搷锛?
```

> 濡傛灉浣犵殑 CI 宸茬粡鎶?tag v1.1.0 鎺ㄥ埌浜?GHCR锛?*骞朵笖**浣犲凡缁忓湪鑷繁 fork 閲屾妸 ha.yml 鏀规垚 `image: ghcr.io/mes/mes-admin:${MES_VERSION}`锛岄偅涔堝彲浠ヨ蛋 B 鏂规锛堜笅闈級銆備絾鍦?*涓讳粨搴?*涓嶈杩欎箞鍋?鈥斺€?ha.yml 鏄璁¤繃鐨勫熀绾裤€?

<details>
<summary>鏂规 B锛堟湭鏉ュ惎鐢紱褰撳墠涓讳粨搴撲笉閫傜敤锛?/summary>

```bash
# 浠呭綋 docker-compose.ha.yml 宸叉敼閫犱负 image: ghcr.io/... 寮曠敤鏃舵墠鍙敤
export MES_VERSION=v1.1.0
for svc in mes-admin mes-gateway mes-system-service mes-master-data-service \
           mes-production-service mes-quality-service mes-material-service \
           mes-integration-service mes-query-service mes-frontend; do
  docker pull ghcr.io/mes/${svc}:${MES_VERSION}
done
# 闅忓悗鍦?搂2.7 鍋?sed 鎵嶆湁鎰忎箟
```

</details>

### 2.7 T+25min 路 鍚屾 compose 涓?.env 鐨勭増鏈敋鐐?

> 鈿狅笍 **P0-3 淇**锛氬綋鍓?`.env` **娌℃湁** `MES_VERSION=` 琛岋紱鍘?Runbook `sed -i MES_VERSION` 鍛戒腑 0 琛屻€乻ed 闈欓粯閫€鍑恒€傞噰鐢ㄦ柟妗?A 鍚?*鏈妭鏁磋妭涓嶅啀鏄搷浣滄楠わ紝鏀逛负涓€娆℃€ф牎楠?*锛?

```bash
# 鏍￠獙 ha.yml 閲岀‘瀹炴病鏈?${MES_VERSION} 寮曠敤锛涜嫢鏈夛紝璇存槑宸插垏鎹负鏂规 B
grep -n '\${MES_VERSION}' docker-compose.ha.yml || echo "[OK] 鏂规 A锛歨a.yml 涓嶄緷璧?MES_VERSION"

# 鏍￠獙闀滃儚鍙 compose 姝ｇ‘ bind锛堝嵆 ha.yml 鐨?service 鍚嶅拰闀滃儚 tag 瀵瑰緱涓婏級
docker compose -f docker-compose.ha.yml config | grep -E '^\s+(image|build):' | head -20
# 鏈熸湜鏂规 A 涓嬬湅鍒板ぇ閲?build: / context: / dockerfile:锛屽嚑涔庢病鏈?image:
```

> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-3 淇**锛毬?.6 浠?10 鏉℃棤鏁?`docker pull ghcr.io/...` 鏀逛负涓€鏉?`docker compose build --pull --parallel`锛浡?.7 浠庢棤鏁?`sed MES_VERSION` 鏀逛负鏂规 A 鐨勪竴娆℃€ч厤缃牎楠屻€傚洖婊氶摼璺?搂4.1 鍋氱浉鍚岃皟鏁淬€傛湭鏉ュ垏鏂规 B 鏃?*鍚屾淇?ha.yml + .env.example 鐨?MES_VERSION 琛?* + 鎭㈠ 搂2.6 / 搂2.7 鍘熸枃銆?

### 2.8 T+28min 路 鍚姩鏂?backend锛堣 Flyway 鑷姩鎵ц migration锛?

**鍙惎鍔?1 鍙?backend**锛岀瓑 Flyway 瀹屾垚鍐嶈捣绗?2 鍙帮紝閬垮厤涓ゅ彴鍚屾椂鍐?`flyway_schema_history` 浜х敓閿佷簤鎶細

```bash
docker compose -f docker-compose.ha.yml up -d mes-backend-1

# 鐩戝惉 Flyway 鏃ュ織
docker compose -f docker-compose.ha.yml logs -f mes-backend-1 | \
  grep -E 'Migrating|Successfully applied|ERROR|FAILED' | \
  tee /var/log/mes-upgrade/$(date +%Y%m%d)/T28-flyway.log
```

鏈熸湜鐪嬪埌渚濇锛?

```
Migrating schema `mes` to version "1.16 - add tenant id to all business tables"
Successfully applied migration ... V1.16 ... (execution time 00:12.345)
Migrating schema `mes` to version "1.17 - fix unique indexes and missing tenant"
Successfully applied migration ... V1.17 ...
Migrating schema `mes` to version "1.18 - production hardening"
Migrating schema `mes` to version "1.19 - dispatch task extension"
Migrating schema `mes` to version "1.20 - must change password"
Migrating schema `mes` to version "2.01 - tenant platform fields"
Migrating schema `mes` to version "2.02 - tenantize rbac"
Migrating schema `mes` to version "2.03 - tenant lifecycle tables"
Migrating schema `mes` to version "2.04 - db defense in depth"
Migrating schema `mes` to version "2.05 - menu permissions"
Schema `mes` contains 11 pending migrations. Successfully applied all migrations.
```

鈿狅笍 **濡傛灉鍗″湪 V1.16 瓒呰繃 15 min**锛氬彲鑳芥槸澶ц〃閿佽〃銆傚鐞嗘楠わ細

1. 鎵撳紑绗簩涓粓绔細`docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW PROCESSLIST\G" | grep -i 'altering\|waiting for'`
2. 濡傛灉纭疄鍦?`altering table`锛岃€愬績绛夊緟锛堝崟琛ㄥ彲鑳?5~20 min锛?
3. 濡傛灉鏄?`Waiting for table metadata lock`锛屽厛 kill 閿佹簮锛歚KILL <thread_id>`
4. 濡傛灉浠嶇劧鍗′綇锛岃 搂鍥?鍥炴粴 SOP

### 2.9 T+45min 路 backend-1 鍋ュ悍妫€鏌?

```bash
# 绛?Spring Boot 鍚姩瀹屾垚锛?5~60s锛?
for i in $(seq 1 20); do
  sleep 5
  status=$(curl -fsS http://mes-backend-1:9090/api/actuator/health 2>/dev/null | jq -r .status)
  if [ "$status" = "UP" ]; then
    echo "[OK] mes-backend-1 UP at $(date +%H:%M:%S)"
    break
  fi
  echo "  waiting... [$i/20] status=$status"
done

# 纭 Flyway 鍏ュ簱浜嗘墍鏈?M8 鐗堟湰
docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" mes \
  -e "SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 15"
```

### 2.10 T+50min 路 鍚姩 backend-2 + gateway 脳2

```bash
docker compose -f docker-compose.ha.yml up -d mes-backend-2

# 绛?backend-2 healthy
for i in $(seq 1 20); do
  sleep 5
  status=$(curl -fsS http://mes-backend-2:9090/api/actuator/health 2>/dev/null | jq -r .status)
  if [ "$status" = "UP" ]; then break; fi
done

docker compose -f docker-compose.ha.yml up -d mes-gateway-1 mes-gateway-2

# Nacos 涓湅鍒版湇鍔℃敞鍐?
curl -s "http://nacos-1:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=20&namespaceId=public" | jq
```

### 2.11 T+60min 路 鍒?MinIO 瀛樺偍锛堝浠?Local 杩佺Щ鍒?MinIO 棣栨涓婄嚎锛?

> 浠呴€傜敤浜?*棣栨**浠?M7锛圠ocalFileServiceImpl锛夊垏鎹㈠埌 M8锛圡inioFileServiceImpl锛夈€傚鏋?M7 宸茬粡鍦ㄧ敤 MinIO锛岃烦杩囥€?
>
> 鈿狅笍 **P0-2 淇**锛歚mc` CLI **涓嶅湪** `mes-minio`锛堝畼鏂?`minio/minio` 闀滃儚 entrypoint 鍙湁 `minio` 浜岃繘鍒讹級銆佷篃**涓嶅湪** `mes-backend-1`锛圝DK 鍩虹闀滃儚鏃?mc锛夈€傚師 Runbook 鐨?`docker exec mes-minio mc ...` 鍜?`docker exec mes-backend-1 mc mirror ...` 浼氭姤 `exec: "mc": executable file not found in $PATH`銆傛纭仛娉曟槸璧蜂竴涓?**鐙珛鐨?`minio/mc` 瀹瑰櫒**鎸傚埌鍚屼竴 docker network锛岄€氳繃 `MC_HOST_*` 鐜鍙橀噺浼犲嚟鎹紝mirror 瀹屽嵆閫€鍑?鈥斺€?闆舵薄鏌撲笖闅旂搴﹂珮銆?

```bash
# 鍓嶇疆鍙橀噺锛堢洿鎺ヤ粠 .env / KMS 璇伙紱绀轰緥涓敤 shell 灞曞紑锛?
: "${MINIO_ROOT_USER:?MINIO_ROOT_USER required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD required}"

# 1) 鐙珛 mc 瀹瑰櫒鍒涘缓 bucket锛?-rm 璺戝畬鑷姩娓呯悊锛屼笉鍗犲鍣ㄦЫ浣嶏級
#    娉ㄦ剰锛氬洜 mes_mes-net 鏄?external network锛圚A compose project=mes锛夛紝
#    --network=mes_mes-net 涓?mes 閮ㄥ垎闇€涓庡疄闄?compose project name 鍖归厤锛?
#    鑻ヤ綘鐢?docker compose -p mes2 ...锛屽簲鏀逛负 --network=mes2_mes-net
docker run --rm --network=mes_mes-net \
  -e MC_HOST_local="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
  minio/mc:latest \
  mb --ignore-existing local/mes local/mes-backups

# 2) 鍏ㄩ噺杩佺Щ鍘嗗彶 uploads 鈥斺€?鎶?mes_uploads 鏁版嵁鍗锋寕杩?mc 瀹瑰櫒
#    mes_uploads 鏄?ha.yml / docker-compose.yml 閲?backend 鐨勫叡浜嵎锛屽瓨鏀?M7 鏈熼棿鐨勬湰鍦颁笂浼?
docker run --rm --network=mes_mes-net \
  -v mes_uploads:/uploads:ro \
  -e MC_HOST_local="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
  minio/mc:latest \
  mirror --overwrite /uploads local/mes 2>&1 | tee \
    /var/log/mes-upgrade/$(date +%Y%m%d)/T60-minio-mirror.log

# 3) 鏍￠獙鏍蜂緥锛堥殢鏈烘娊 20 涓枃浠讹紝纭钀界洏涓€鑷达級
docker run --rm --network=mes_mes-net \
  -v mes_uploads:/uploads:ro \
  -e MC_HOST_local="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
  --entrypoint sh \
  minio/mc:latest \
  -c 'find /uploads -type f | shuf | head -20 | while read f; do
        rel=${f#/uploads/}
        mc stat "local/mes/${rel}" >/dev/null 2>&1 && echo "[OK] $rel" || echo "[MISS] $rel"
      done'

# 4) 鏍￠獙 bucket 鎬诲ぇ灏忎笌鏈湴鍗蜂竴鑷达紙quick sanity锛?
docker run --rm --network=mes_mes-net \
  -e MC_HOST_local="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
  minio/mc:latest \
  du -h local/mes | tee \
    /var/log/mes-upgrade/$(date +%Y%m%d)/T60-minio-du.log
```

> **涓轰粈涔堜笉 docker exec mes-backend-1 mc ...**锛?
> 1. Spring Boot 涓氬姟闀滃儚閲屾斁 CLI 宸ュ叿浼氳鏀诲嚮闈㈡墿澶э紙mcp12 鐨?security-audit 鏄庣‘瑕佹眰涓氬姟瀹瑰櫒鍙暀 jar + JRE锛夛紱
> 2. mcp25 P1-16 HA 鎶ュ憡閲?backend 宸茬粡 `USER appuser:10001`锛屾病鏈?apk add 鏉冮檺锛岃繍琛屾椂瑁呬笉浜?mc锛?
> 3. 瀹樻柟 `minio/mc` 2.7 MB锛屼复鏃?`docker run --rm` 鍚仠 < 1 绉掞紝姣?sidecar 妯″紡鏇磋交銆?
>
> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-2 淇**锛歚docker exec mes-minio mc ...` / `docker exec mes-backend-1 mc ...` 鍏ㄩ儴鎹㈡垚 `docker run --rm --network=mes_mes-net minio/mc:latest ...`锛岀敤 `MC_HOST_local=http://user:pass@minio:9000` 浼犲嚟鎹紱鎸?`mes_uploads` volume 鍋?mirror 婧愶紝鏃ュ織鎸佷箙鍖栧埌 `/var/log/mes-upgrade/`銆?

### 2.12 T+75min 路 鏇存柊鍓嶇

```bash
docker compose -f docker-compose.ha.yml up -d mes-frontend
curl -fsS http://mes-frontend/health   # 棰勬湡 "ok"
```

### 2.13 T+80min 路 鎭㈠涓氬姟娴侀噺

> 鈿狅笍 **P0-4 缁?*锛氬悓 搂2.1锛宍nginx -s reload -c <file>` 鏃犳晥銆傜敤 `cp + reload` 鍒囧洖 `nginx.ha.conf`锛堟垨鍒囧埌 `nginx.microservice.conf` / `nginx.conf`锛屾寜浣犵殑閮ㄧ讲褰㈡€侀€夛級銆?

```bash
# 濡傛灉涔嬪墠鍒囧埌缁存姢椤碉紝鎭㈠
docker exec mes-frontend sh -c '
  cp /etc/nginx/conf.d/nginx.ha.conf /etc/nginx/conf.d/default.conf &&
  nginx -t &&
  nginx -s reload
'

# 绔嬪埢鏍￠獙 /api/ 鍥炲埌 upstream锛堝簲鏄?502/504 绛変笉鍐嶆槸 503 inline锛夋垨鑰?2xx锛堣嫢 backend 宸插仴搴凤級
curl -sS -o /dev/null -w '%{http_code}\n' http://localhost/health
# 鏈熸湜 200 "ok"锛坣ginx.ha.conf 鐨?/health 杩斿洖 ok锛?
```

> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-4 缁慨璁?*锛氫笌 搂2.1 瀵圭О锛屽垏鍥?`nginx.ha.conf` 鍚屾牱鐢?`cp + reload`銆?

### 2.14 T+85min 路 AlertManager 闈欓粯瑙ｉ櫎

> 鈿狅笍 **P0-1 缁?*锛氭姝ラ渶瑕?monitoring 鏍堜粛鐒?up锛堣 搂1.5 鏂板鍓嶇疆锛夈€傝嫢杩愮淮鍦?搂1.5 蹇樿鍚洃鎺ф爤锛岃繖閲岀殑 curl 浼?connection refused锛屽缓璁ˉ涓€鏉?connectivity 妫€鏌ュ啀璋冪敤銆?

```bash
# 杩為€氭€ч妫€锛堢‘淇?monitoring 鏍堟椿鐫€锛?
curl -fsS http://localhost:9093/-/healthy || {
  echo "[FATAL] alertmanager 涓嶅彲杈撅紱璇峰厛 docker compose -f docker-compose.ha.yml -f docker-compose.monitoring.yml up -d mes-alertmanager"
  exit 1
}

silence_id=$(curl -s http://localhost:9093/api/v2/silences | jq -r '.[] | select(.comment | startswith("M8 upgrade window")) | .id')
[ -n "$silence_id" ] && curl -X DELETE "http://localhost:9093/api/v2/silence/${silence_id}"
# 娉細鍘?Runbook 鐢?`comment=="M8 upgrade window; auto-expire 2026-04-22 05:00"`锛堢‖缂栫爜鏃ユ湡锛?
# 鏀规垚 `startswith("M8 upgrade window")` 璁╄剼鏈浠绘剰鍗囩骇澶滈兘閫傜敤锛堝搴?dry-run P3-2 椤烘墜淇級
```

> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-1 缁?+ P3-2 淇**锛氬姞 alertmanager 杩為€氭€ч妫€锛涙妸 jq 鐨勬棩鏈熺‖缂栫爜鎹㈡垚鍓嶇紑鍖归厤銆?

---

## 涓夈€佸仴搴锋鏌ヤ笌鍐掔儫锛圱+90 鈫?T+120min锛?

### 3.1 閾捐矾 1锛氱櫥褰?

```bash
TOKEN=$(curl -fsS -X POST http://mes-frontend/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"'"$ADMIN_PROD_PASSWORD"'","tenantCode":"default"}' | \
  jq -r .data.accessToken)

[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && echo "[OK] 鐧诲綍"
```

### 3.2 閾捐矾 2锛氬伐鍗曞垪琛?

```bash
curl -fsS "http://mes-frontend/api/v1/workorder/work-order/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.total'
```

### 3.3 閾捐矾 3锛氭淳宸ヤ换鍔″垎椤?

```bash
curl -fsS "http://mes-frontend/api/v1/dispatch/task/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.total'
```

### 3.4 閾捐矾 4锛氬畬宸ュ叆搴擄紙GET 鍒楄〃 + 鍐欐帴鍙?DTO 鏍￠獙锛?

```bash
curl -fsS "http://mes-frontend/api/v1/material/finished-goods-receipt/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.total'

# 鐢ㄦ柊 DTO 濂戠害璇曞垱寤轰竴鏉★紙鍔″繀浣跨敤 smoke 宸ュ崟 ID锛?
curl -fsS -X POST http://mes-frontend/api/v1/material/finished-goods-receipt \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiptType":"FIN","warehouse":"WH-SMOKE","movementType":"IN","planReceiptTime":"2026-04-22 10:00:00","items":[{"materialCode":"SMOKE-001","receiptQty":1,"unit":"PCS"}]}'
```

### 3.5 閾捐矾 5锛氬璁℃棩蹇楀彲瑙?

```bash
curl -fsS "http://mes-frontend/api/v1/audit/log?pageNum=1&pageSize=10&action=POST_%25" \
  -H "Authorization: Bearer $TOKEN" | \
  jq '.data.list | length'
# 棰勬湡 >= 3锛堝惈鍒氭墠鐨勭櫥褰?+ 鍏ュ簱 + ...锛?
```

### 3.6 閾捐矾 6锛歁ES 鈫?APS HMAC 澶栧懠

```bash
# 瑙﹀彂涓€娆″畾鏃?ApsUpstreamSyncJob 鎵嬪姩鎵ц
curl -fsS -X POST "http://mes-frontend/api/v1/aps/sync/orders/trigger" \
  -H "Authorization: Bearer $TOKEN"

# 3 绉掑悗鐪?ApsClient 鏃ュ織
docker logs mes-backend-1 --since=10s | grep 'X-External-Request-Id='
# 棰勬湡鐪嬪埌锛欰PS 澶栧懠璇锋眰澶?X-External-Request-Id=MES-<tenantId>-<UUID>
```

### 3.7 鎸囨爣 & 鍛婅鑷

> 鈿狅笍 **P0-1 缁?/ P1-5**锛氭姝ュ亣璁?monitoring 鏍堝凡鍙犲姞鍚姩锛埪?.5 鍓嶇疆锛夈€俶cp25 v3 瀹炴祴锛氶娆?`up` 瀹屾暣鍖归厤闇€瑕?**Prometheus 鍚姩 鈮?60 绉?*锛坰crape_interval=15s 鑷冲皯璺?2 杞噰闆嗭級 + 涓嬫父 exporter 鍏ㄩ儴 up锛屽洜姝ゆ湰姝ユ渶鏃╁湪 T+91min 涔嬪悗鎵ц锛屼笖 `up=1` 鐨勭悊鎯虫槸"鑷冲皯 鈮?鍩虹璁炬柦 exporter 鏁伴噺"锛岃€屼笉鏄?19/19銆?

```bash
# 棰勭疆锛氱瓑 Prometheus 杩炵画閲囬泦 60 绉掞紙涓や釜 scrape 鍛ㄦ湡锛?
for i in 1 2 3 4; do
  up=$(curl -s 'http://localhost:9091/api/v1/query?query=count(up)' | jq -r '.data.result[0].value[1]')
  echo "  [t+$((i*15))s] total targets = $up"
  sleep 15
done

# Prometheus 鐔熸倝鐨?up 鎸囨爣
curl -s 'http://localhost:9091/api/v1/query?query=up{job=~"mes-.*"}' | \
  jq '.data.result[] | {job: .metric.job, instance: .metric.instance, val: .value[1]}'

# 鏈熸湜锛圚A 鍗曚綋閮ㄧ讲锛夛細
#   cadvisor=1 / prometheus(self)=1 / mes-admin 鎴?mes-backend-ha * 2 = 2 / mes-gateway=1 鎴?2
#   mysql-exporter=1 / redis-exporter=1 / rabbitmq=1 / nacos=1
#   鍚堣鑷冲皯 8~10 涓?up=1
#   node-exporter锛歀inux=1锛沇indows Docker Desktop 涓嶅吋瀹?rslave mount 涓?0锛堣 mcp25 v3 搂涓?3锛?

# 鍙嶈繃鏉ユ煡 down 鐨勶紝鐢ㄤ簬鍒ゅ畾鍝簺 exporter 娌¤捣
curl -s 'http://localhost:9091/api/v1/query?query=up==0' | \
  jq '.data.result[] | .metric.job + " / " + .metric.instance'

# 鏁呮剰瑙﹀彂涓€娆′笟鍔?5xx 鍛婅锛堣烦杩囷紝鍙湪 staging 鍋氾級
```

> **鏈妭鎹?`fix-mcp21-m8-runbook-dryrun.md` P0-1 缁?+ P1-5 淇**锛氫笉鍐嶈姹?鎵€鏈?mes-* up=1"锛堥鍚笉鐜板疄锛夛紝鏀逛负"鍏堢瓑 60s 閲囬泦 + 鏌?up{} 缁嗚妭"锛涘榻?mcp25 v3 瀹炴祴缁撹锛堥鍚彧鏈?cadvisor up=1锛夈€?

### 3.8 CI 鐨?Playwright 鍙€夊洖鏀撅紙浠?staging锛?

```bash
# 鍦?staging 鐜涓婅窇 15 鏉?E2E锛堢敓浜т笉璺戯紝閬垮厤閫犳暟鎹級
cd mes-frontend
E2E_BASE=http://mes-staging/api \
E2E_USER=admin E2E_PASS="$STAGING_ADMIN_PWD" E2E_TENANT=default \
npx playwright test
```

---

## 鍥涖€佸洖婊?SOP

**瑙﹀彂鏉′欢锛堜换涓€锛?*锛?

- 搂浜?浠讳竴姝ュけ璐ヤ笖鏃犳硶 10 min 鍐呰В鍐?
- 搂涓?鍐掔儫 5 鏉￠噷鏈?2 鏉″け璐?
- AlertManager 鎸佺画 10 min P0 鍛婅

### 4.1 鍥炴粴姝ラ锛堢洰鏍?鈮?30 min锛岀湡瀹炰及绠楄 搂4.3锛?

```bash
export PREV_VERSION=v1.0.7

# 1) 鍋滄柊鐗堟湰瀹瑰櫒锛堜繚鐣欐暟鎹嵎锛?
docker compose -f docker-compose.ha.yml stop \
  mes-backend-1 mes-backend-2 mes-gateway-1 mes-gateway-2 mes-frontend

# 2) 鍒囬暅鍍?tag 鍒板墠涓€鐗堟湰
# 鈿狅笍 P0-3 缁細鏂规 A锛坔a.yml 涓?build-from-source锛変笅锛宻ed MES_VERSION 鏃犳晥锛?
#    姝ゆ椂鍥炴粴闈?git checkout 鍒板崌绾у墠 tag锛屽啀 docker compose build --pull --parallel
#    鏂规 B锛堟湭鏉?ha.yml 璧?Registry image锛変笅锛屾墠淇濈暀 sed 鏂瑰紡銆?
if grep -q '\${MES_VERSION}' docker-compose.ha.yml; then
  # 鏂规 B锛歊egistry 妯″紡锛堟湭鏉ワ級
  sed -i "s|MES_VERSION=.*|MES_VERSION=${PREV_VERSION}|" .env
  grep '^MES_VERSION' .env
else
  # 鏂规 A锛氬綋鍓嶄富浠撳簱鐜扮姸 鈥斺€?鍒?git tag + rebuild
  git fetch --tags
  git checkout "${PREV_VERSION}"   # 渚嬪 v1.0.7
  docker compose -f docker-compose.ha.yml build --pull --parallel 2>&1 | tee \
    /var/log/mes-upgrade/$(date +%Y%m%d)/rollback-image-build.log
fi

# 3) 浠庡崌绾х獥鍙ｅ墠鐨勫浠芥仮澶?MySQL锛堝鏋?Flyway 鐮村潖浜?schema锛?
# 鈿狅笍 P1-2锛氳嫢 .env 鏈 BACKUP_GPG_PASSPHRASE锛屽浠戒骇鐗╂槸 .sql.gz锛堟棤 .gpg 鍚庣紑锛?
#    鍏煎涓ょ鎵╁睍鍚嶏細
BACKUP_PATTERN="/backup/mysql/daily/mes_daily_$(date +%Y%m%d)_*.sql.gz"
DUMP_FILE=$(ls -1t ${BACKUP_PATTERN}.gpg ${BACKUP_PATTERN} 2>/dev/null | head -1)
[ -z "$DUMP_FILE" ] && { echo "[FATAL] 鎵句笉鍒板浠?dump"; exit 1; }
echo "浣跨敤 dump锛?DUMP_FILE"

bash scripts/backup/mysql-restore.sh \
  --dump="$DUMP_FILE" \
  --binlog-dir=/backup/mysql/binlog \
  --target-time="$(cat /var/log/mes-upgrade/$(date +%Y%m%d)/T5-binlog.log | grep -oP 'File:\s*\K\S+')"

# 鈿狅笍 濡傛灉 Flyway 鏀逛簡 schema 浣嗕笟鍔″皻鏈啓鏂版暟鎹紝鍙敤浠ヤ笅杞婚噺鍥炴粴锛?
# docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" mes \
#   -e "DELETE FROM flyway_schema_history WHERE version IN ('1.16','1.17','1.18','1.19','1.20','2.01','2.02','2.03','2.04','2.05');"
# 鐒跺悗鎵嬪伐 undo V1.16 鐨?tenant_id 鍒?/ V2.02 鐨?PK 鏀瑰姩 / V2.05 鐨?sys_menu 鎻掑叆锛堟厧鐢紒锛?

# 4) 鍚姩鑰佺増鏈?
docker compose -f docker-compose.ha.yml up -d mes-backend-1 mes-backend-2
# ... 鎸?搂2.9 / 搂2.10 鍋ュ悍妫€鏌?...
docker compose -f docker-compose.ha.yml up -d mes-gateway-1 mes-gateway-2 mes-frontend

# 5) 楠岃瘉 搂涓?鐨?5 鏉￠摼璺娇鐢ㄨ€佺増鏈€昏緫鍧囪兘閫氳繃
```

### 4.2 鍥炴粴鍚庣殑鍔ㄤ綔

- [ ] 15 鍒嗛挓鍐呭湪杩愮淮缇?@ 鎵€鏈変汉锛?*"M8 鍗囩骇宸插洖婊氾紝褰撳墠杩愯 ${PREV_VERSION}"**
- [ ] 鎶婂け璐?log + flyway-schema-history + binlog 浣嶇偣鎵撳寘缁?mcp9 / mcp11 鍒嗘瀽
- [ ] 48h 鍐呭鐩樹細 鈫?鍐欎簨鏁呮姤鍛?鈫?鎵炬敼杩涢」
- [ ] 涓嬩竴娆″崌绾х獥鍙ｅ墠锛屾敼杩涢」蹇呴』鍏ㄩ儴楠岃瘉閫氳繃

---

## 浜斻€佸父瑙佸崌绾ф晠闅?Troubleshoot

> 鏈珷涓?`docs/operations/runbook.md` **浜掕ˉ**锛歳unbook 闈㈠悜杩愯鏈熶簨鏁咃紱鏈妭闈㈠悜鍗囩骇鏈熶簨鏁呫€?

### 5.1 闂锛欶lyway V1.16 鎵ц > 20 min 鏃犺繘灞?

**鐥囩姸**锛歚Migrating schema mes to version "1.16 - add tenant id to all business tables"` 闀挎椂闂存棤鍚庣画鏃ュ織銆?

**鎺掓煡**锛?

```bash
docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" mes \
  -e "SHOW PROCESSLIST;" | grep -i 'altering\|waiting'

docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "SELECT * FROM information_schema.innodb_trx\G"
```

**澶勭悊**锛?

- 濡傛灉鐪熷湪 ALTER锛坄altering table`锛夛紝鑰愬績绛夛紙澶ц〃鍙兘 10~30 鍒嗛挓锛夛紱鏈熼棿鍚屾瑙傚療 disk I/O
- 濡傛灉鍦?`Waiting for table metadata lock`锛屼紭鍏?KILL 鎸侀攣浜嬪姟锛?

  ```sql
  KILL <blocker_trx_id>;
  ```

- 濡傛灉 binlog 鍐欐弧浜嗙鐩橈紝鍏堟墿 `/var/lib/mysql` 鎴栨竻鏃?binlog锛?

  ```bash
  docker exec mes-mysql-primary mysql -uroot -p... -e "PURGE BINARY LOGS BEFORE DATE_SUB(NOW(), INTERVAL 2 DAY);"
  ```

### 5.2 闂锛歜ackend 鍚姩澶辫触骞舵姤 `ProdEnvValidator` 閿欒

**鐥囩姸**锛氭棩蹇楀嚭鐜帮細

```
[P0-05] 鐢熶骇鐜閰嶇疆鏍￠獙澶辫触锛歋PRING_DATASOURCE_PASSWORD 鍛戒腑寮卞€?...
[P1-36] 鍚敤 APS 闆嗘垚鏃跺繀椤昏缃?mes.aps.hmac-key锛堢幆澧冨彉閲?MES_APS_HMAC_KEY锛夛紝闀垮害 >= 32
```

**澶勭悊**锛?

- 杩欐槸 M8 鏂板鐨勫畨鍏ㄧ孩绾匡紝**涓嶆槸 bug**
- 鍥炲埌 搂1.3 鏍￠獙鐜鍙橀噺锛屾妸 `.env` / KMS secret 琛ラ綈閲嶅惎鍗冲彲
- **涓ョ**鎶?ProdEnvValidator 鍏虫帀锛坄mes.env-validator.enabled=false`锛夛紱杩欐槸鏈€鍚庝竴閬撻槻绾?

### 5.3 闂锛?actuator/health 鎸佺画 starting

**鐥囩姸**锛歜ackend 瀹瑰櫒 `health: starting` 涓嶅彉缁裤€?

**鎺掓煡椤哄簭**锛?

1. `docker logs mes-backend-1 --tail=100` 鐪嬫槸鍚︽湁 `Communications link failure`
2. `docker exec mes-backend-1 curl -v mysql-primary:3306` 鐪嬬綉缁滈€氫笉閫?
3. `docker exec mes-mysql-primary mysql -e 'SELECT 1'` 鐪?MySQL 鏈韩 OK 涓?
4. 濡傛灉鏄?Nacos 娉ㄥ唽瓒呮椂锛圚A 闆嗙兢 3 鑺傜偣鏈?1 鎸傦級锛屾煡 `docker logs mes-nacos-1`

### 5.4 闂锛歶ploads 鍒?MinIO 鍚庡墠绔湅涓嶅埌鍘嗗彶鍥剧墖

**鐥囩姸**锛歚mc mirror` 瀹屽悗璁块棶 `/files/tenant-1/...` 杩斿洖 404銆?

**澶勭悊**锛?

- 纭 `MES_FILE_STORAGE_TYPE=minio` 宸叉敞鍏ワ紝backend 閲嶅惎
- 鏃ュ織搴旂湅鍒?`MinIO 鏂囦欢鏈嶅姟鍒濆鍖栧畬鎴? endpoint=http://minio:9000, bucket=mes`
- 鍓嶇璇锋眰鐨?URL 姝ゆ椂鍙樻垚 `minio://mes/...`锛屽墠绔細閫氳繃 `getUrl(logicalUrl, 3600)` 鎹㈢鍚?URL
- 濡傛灉鐢ㄦ埛杩樺湪鐪嬫棫 URL锛堟祻瑙堝櫒缂撳瓨锛夛紝璁╃敤鎴峰埛鏂伴〉闈?

### 5.5 闂锛歅laywright 鍦?staging 璺戝け璐?

**鐥囩姸**锛歚login.spec.ts - 姝ｇ‘鍑嵁鐧诲綍鎴愬姛 + 鐧诲嚭` 鎶?401銆?

**澶勭悊**锛?

- 纭 staging 鐨?admin 瀵嗙爜宸茬粡閫氳繃 "must_change_password" 棣栨鏀瑰瘑娴佺▼
- 鎴栦复鏃舵妸 staging 鐨?admin 鐨?`must_change_password=0` 璺戝畬 E2E 鍐嶆敼鍥炴潵锛?

  ```sql
  UPDATE sys_user SET must_change_password=0 WHERE username='admin' AND tenant_id=1;
  ```

### 5.6 闂锛氱櫥褰曟椂琚?HmacSignatureFilter 401

**鐥囩姸**锛歚/auth/login` 杩斿洖 401 + `APS 鍥炶皟绛惧悕涓嶅尮閰峘銆?

**澶勭悊**锛?

- `HmacSignatureFilter` 鍙 `/aps/callback/**` 璧锋晥锛屼笉搴旀嫤 `/auth/login`
- 鑻ョ‘瀹炶鎷︿簡锛屾槸 `shouldNotFilter` 璺緞鍖归厤鍑洪敊锛屾鏌?`ServletRequest.getServletPath()` 鏄惁琚墠绔弽浠ｆ敼璺緞
- 鎺掓煡 nginx 閰嶇疆閲岀殑 `rewrite` 瑙勫垯

---

## 鍏€佸崌绾у悗 24h 瑙傚療娓呭崟

| 鏃堕棿 | 椤?| 璐熻矗 |
| :---: | --- | --- |
| T+2h | AlertManager 闈㈡澘 0 鏉?P0/P1 | 鍊肩彮 on-call |
| T+4h | `sys_audit_log` 褰撴棩琛屾暟 > 100 | 涓氬姟 Lead锛堣鏄庝笟鍔＄湡鍦ㄨ浣跨敤锛?|
| T+4h | `actuator/prometheus` 鎸囨爣姝ｅ父锛歚jvm.memory.used < 80%`銆乣mysql_global_status_threads_connected < 150` | SRE |
| T+6h | Playwright CI 姣?6h 璺戜竴娆★紝鍏ㄧ豢 | CI |
| T+12h | Nacos 娉ㄥ唽瀹炰緥鏁颁笌棰勬湡涓€鑷达紙HA 鐗?backend脳2 + gateway脳2 + integration脳1锛?| SRE |
| T+18h | 褰撴棩 03:00 MySQL full backup 鎴愬姛 + 寮傚湴涓婁紶瀹屾垚 | 澶囦唤鐩戞帶 |
| T+24h | 澶嶇洏浼氾細鏈鍗囩骇 4 涓渶澶?pain point + 涓嬫鏀硅繘 | 鏁寸粍 |

---

## 闄勫綍 A 路 鐜鍙橀噺娓呭崟锛圡8 鍏ㄩ噺锛?

| 绫诲埆 | 鍙橀噺 | 蹇呴渶 | 璇存槑 |
| --- | --- | :---: | --- |
| DB | `MYSQL_ROOT_PASSWORD` | 鉁?| 闈炲急鍊硷紝`ProdEnvValidator` 浼氭 |
| DB | `MYSQL_REPLICATION_PASSWORD` | HA 鉁?| 涓讳粠澶嶅埗璐﹀彿瀵嗙爜 |
| DB | `SPRING_DATASOURCE_URL` | 鉁?| jdbc:mysql://mysql-primary:3306/mes... |
| DB | `SPRING_DATASOURCE_USERNAME` | 鉁?| mes_app |
| DB | `SPRING_DATASOURCE_PASSWORD` | 鉁?| 闈炲急鍊?|
| Redis | `REDIS_PASSWORD` | 鉁?| 闈炵┖ |
| RabbitMQ | `SPRING_RABBITMQ_ADDRESSES` | HA 鉁?| `rabbitmq-1:5672,rabbitmq-2:5672,rabbitmq-3:5672` |
| RabbitMQ | `SPRING_RABBITMQ_USERNAME` | 鉁?| mes |
| RabbitMQ | `SPRING_RABBITMQ_PASSWORD` | 鉁?| 闈炲急鍊?|
| RabbitMQ | `RABBITMQ_ERLANG_COOKIE` | HA 鉁?| 璺ㄨ妭鐐圭浉鍚岄殢鏈轰覆 |
| Nacos | `NACOS_SERVER_ADDR` | HA 鉁?| `nacos-1:8848,nacos-2:8848,nacos-3:8848` |
| Nacos | `NACOS_AUTH_TOKEN` | 鉁?| Base64(鈮?2 瀛楄妭闅忔満) |
| Nacos | `SPRING_CLOUD_NACOS_DISCOVERY_USERNAME` | 鉁?| nacos 鐢ㄦ埛鍚?|
| Nacos | `SPRING_CLOUD_NACOS_DISCOVERY_PASSWORD` | 鉁?| nacos 瀵嗙爜 |
| JWT | `MES_JWT_SECRET` | 鉁?| 鈮?2 瀛楄妭闅忔満 |
| APS | `MES_APS_API_KEY` | 鉁?| 鈮?6锛屼笉鍛戒腑寮卞€奸粦鍚嶅崟 |
| APS | `MES_APS_HMAC_KEY` | 鉁?| 鈮?2 |
| APS | `APS_BASE_URL` | 鉁?| 瀵圭 APS Gateway |
| APS | `APS_OUTBOUND_API_KEY` | 鉁?| MES鈫扐PS 鍑虹珯 API Key |
| MinIO | `MES_FILE_STORAGE_TYPE` | 鉁?| `minio` |
| MinIO | `MES_FILE_MINIO_ENDPOINT` | 鉁?| http://minio:9000 |
| MinIO | `MES_FILE_MINIO_BUCKET` | 鉁?| mes |
| MinIO | `MES_FILE_MINIO_ACCESS_KEY` | 鉁?| 闈?minioadmin |
| MinIO | `MES_FILE_MINIO_SECRET_KEY` | 鉁?| 寮洪殢鏈?|
| MinIO | `MINIO_ROOT_USER` | 鉁?| 闈?minioadmin |
| MinIO | `MINIO_ROOT_PASSWORD` | 鉁?| 寮洪殢鏈?鈮?6 |
| 鐩戞帶 | `GRAFANA_PASSWORD` | 鉁?| 闈?admin |
| 鐩戞帶 | `DINGTALK_ROBOT_P0_URL` | 鉁?| 閽夐拤 webhook |
| 鐩戞帶 | `DINGTALK_ROBOT_P1_URL` | 鈿狅笍 | 閽夐拤 webhook |
| 鐩戞帶 | `FEISHU_ROBOT_URL` | 鈿狅笍 | 椋炰功 webhook锛堝彲閫夛級 |
| ES | `MES_ES_ENABLED` | 鈿狅笍 | 榛樿 false锛涘闇€鍚敤 |
| ES | `MES_ES_URIS` | ES 鍚敤 鉁?| `http://es01:9200,http://es02:9200,http://es03:9200` |
| ES | `MES_ES_USERNAME` / `MES_ES_PASSWORD` | ES 鍚敤 鉁?| elastic |
| 澶囦唤 | `BACKUP_GPG_PASSPHRASE` | 鉁?| KMS 鎵樼锛屾案涓嶄笌澶囦唤鍚屽簱 |
| 鐜 | `SPRING_PROFILES_ACTIVE` | 鉁?| `prod` |
| 鐜 | `MES_ENV` | 鉁?| `prod` |
| 鐜 | `TZ` | 鉁?| `Asia/Shanghai` |

> **锛圥1-6 閰嶅锛?* 宸ョ▼鏍?`.env.example` 鏂囦欢宸茶鐩栨湰闄勫綍鐨勫叏閮ㄥ彉閲忥紝cp 鍒?`.env` 鍐嶆寜 KMS 璁板綍鏇挎崲 `REPLACE_ME_...` 鍗犱綅鍗冲彲銆傝剼鏈浠界嫭绔?env 浠嶅湪 `scripts/backup/.env.example`锛堝眬閮ㄨ鐩?BACKUP_* 鍓嶇紑鍙橀噺锛夛紝涓よ€呬笉鍐茬獊銆傛鍓?.env 缂哄け鐨?8 涓繀闇€椤癸紙`APS_BASE_URL` / `APS_OUTBOUND_API_KEY` / `MES_FILE_MINIO_ACCESS_KEY` / `MES_FILE_MINIO_SECRET_KEY` / `DINGTALK_ROBOT_P0_URL` / `DINGTALK_ROBOT_P1_URL` / `MES_ENV` / `BACKUP_GPG_PASSPHRASE`锛夊潎宸插湪 `.env.example` 琛ラ綈锛沗MES_FILE_STORAGE_TYPE` 榛樿鍊间粠 `local` 璋冩暣涓?`minio`锛堝榻?搂2.11 棣栨杩佺Щ鐩爣锛夈€?
>
> **鏈檮褰曟嵁 `fix-mcp21-m8-runbook-dryrun.md` P1-6 淇**锛氬師闄勫綍 A 鍙垪"鍙橀噺搴旇瀛樺湪"锛屼絾娌℃湁閰嶅 `.env.example`锛涚幇鎻愪緵宸ョ▼鏍?`.env.example`锛孋I 鍙姞 `diff <(grep -E '^[A-Z_]+=' .env.example | sort) <(grep -E '^[A-Z_]+=' .env | sort)` 闃叉紓绉汇€?

---

## 闄勫綍 B 路 浜哄憳涓庢矡閫氱煩闃?

| 瑙掕壊 | 濮撳悕 | 鑱旂郴鏂瑰紡 | 涓婄嚎绐楀彛鏈熷€煎畧 |
| --- | --- | --- | :---: |
| 涓婄嚎璐熻矗浜?| _____ | _____ | 鉁?|
| DBA on-call | _____ | _____ | 鉁?|
| Backend on-call | _____ | _____ | 鉁?|
| Frontend on-call | _____ | _____ | 瑙嗘儏鍐?|
| DevOps on-call | _____ | _____ | 鉁?|
| 瀹夊叏 on-call | _____ | _____ | 鉁?|
| 涓氬姟 Lead | _____ | _____ | 鉁?|
| CTO / 鍐崇瓥灞?| _____ | _____ | 闅忓彨闅忓埌 |

**鍗囩骇鏈熼棿娌熼€氱兢**锛歚MES-M8-UPGRADE-War-Room`锛堜复鏃剁兢锛孴-24h 鍒涘缓锛孴+48h 褰掓。锛?

---

## 闄勫綍 C 路 蹇€熷懡浠ゅ崱

> 鎵撳嵃鍑烘潵鏀惧湪 DevOps 妗屼笂锛屼换浣曚竴姝ュ崱浣忓厛鎵竴閬嶃€?

```bash
# Flyway 鐘舵€?
docker run --rm --network=mes_mes-net \
  -e FLYWAY_URL=jdbc:mysql://mysql-primary:3306/mes \
  -e FLYWAY_USER=mes_app -e FLYWAY_PASSWORD="$MES_APP_PASSWORD" \
  -v $(pwd)/sql:/flyway/sql:ro flyway/flyway:10-alpine info

# MySQL 杩炴帴 / 杩涚▼
docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW PROCESSLIST\G"

# 鏈嶅姟鍋ュ悍
for host in mes-backend-1 mes-backend-2 mes-gateway-1 mes-gateway-2; do
  echo -n "$host: "
  docker exec $host curl -fsS http://localhost:9090/actuator/health | jq -r .status
done

# Nacos 娉ㄥ唽
curl -s "http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=20" | jq

# AlertManager 褰撳墠鍛婅
curl -s http://localhost:9093/api/v2/alerts | jq '.[] | {name: .labels.alertname, severity: .labels.severity}'

# 鎵嬪姩瑙﹀彂 AlertManager 娴嬭瘯
docker exec mes-alertmanager amtool alert add alertname=UpgradeSmokeTest severity=P2 --generator-url=http://test

# MinIO 妗?/ 澶囦唤
docker exec mes-minio mc ls local/mes/ | head
docker exec mes-minio mc ls local/mes-backups/daily/ | tail -5

# MES 瀹¤鏃ュ織
docker exec mes-mysql-primary mysql -uroot -p"$MYSQL_ROOT_PASSWORD" mes \
  -e "SELECT COUNT(*), MAX(created_time) FROM sys_audit_log WHERE DATE(created_time)=CURDATE()"
```

---

**锛圡8 涓婄嚎鍗囩骇鍓ф湰瀹岋級**

> 濡傚彂鐜版湰 Runbook 涓庡疄闄呯幆澧冧笉涓€鑷达紝浼樺厛璋冩暣 Runbook 骞惰褰曞埌 `docs/test-reports/fix-mcp12-runbook-fix.md`锛堟垨绫讳技鏂囦欢锛夛紝涓嶈"鍑粡楠?鏀规搷浣滄楠ゃ€?
