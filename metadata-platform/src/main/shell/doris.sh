#!/bin/bash

DATAX_HOME=/opt/module/datax
DATAX_LOG_HOME=/data/datax/logs
SQL_PATH=/opt/module/datax/script
default_sink_db="ods"
default_split_pk=1
default_channel=10
min_date="2023-06-01"
taskname=$1
startdate=$2
initdate=$3
label_name=${taskname}_$(uuidgen | tr -d "-")
next_day=`date -d "$startdate +1 day" +%Y-%m-%d`
yesterday=`date -d yesterday +%Y-%m-%d`
partition_day="p_"`date -d "$startdate" +%Y%m%d`
if [[ ${startdate}x == x ]];then startdate=$yesterday;fi
declare -A map_http
separator_line="|"
separator_column="^"
separator_comma=","
words=""
column=""
front="["
back="]"
sign=""
#获取前缀
profix=`echo $taskname | awk -F _ '{print $1}'`
temp_time=$[$(date +%s%N)/1000000]
temp_shell_file_dir=$DATAX_LOG_HOME/`date +%Y-%m-%d`
if [ ! -f "${temp_shell_file_dir}" ];then
mkdir -p ${temp_shell_file_dir}
fi
is_transform_type(){
  case $1 in
    "char")
        echo "STRING"
        ;;
    "varchar")
        echo "STRING"
        ;;
    "tinytext")
        echo "STRING"
        ;;
    "mediumtext")
        echo "STRING"
        ;;
    "text")
        echo "STRING"
        ;;
    "longtext")
        echo "STRING"
        ;;
    "character")
        echo "STRING"
        ;;
    "character varying")
        echo "STRING"
        ;;
    *)
    echo "no"
    ;;
  esac
}
function runOds(){
##新增跳过ods层功能
if [[ `mysql --login-path=scheduler_meta -e "SELECT cnf_value from scheduler_meta.common_config where cnf_key=2" | awk 'NR==2{print}'` == 1 ]];then
  echo "开启跳过ods!!!";
  exit 0;
fi
##获取配置元数据
##增加字段初始化标记
result=$(mysql --login-path=scheduler_meta -e "SELECT d.hosts,d.port,d.user_name,d.password,d.db_type,t.db_name,t.table_name,t.initial_flag,t.config from scheduler_meta.table_info t left join scheduler_meta.db_info d on t.server_name =d.server_name where t.task_name =trim('$taskname')")
if [[ ${result}x == x ]];then
echo "ERROR: taskname not configured"
exit 4
fi
while read -r f1 f2 f3 f4 f5 f6 f7 f8 f9; do hosts="$f1" port="$f2" user_name="$f3" password="$f4" db_type="$f5" db_name="$f6" table_name="$f7" initial_flag="$f8" config="$f9";done <<< "$result"
if [[ ${config}x == x ]];then config="{}";fi
if [[ ${config}x == NULLx ]];then config="{}";fi
sink_table=`[[ $(echo $config |jq '.sink_table') = null ]] && echo $taskname || echo $(echo $config |jq '.sink_table' | awk -F '"' '{print $2}')`
start_dt=`[[ ${initial_flag} == 1 ]] && echo $startdate || echo $initdate`
#加上分表逻辑
if [[ ${table_name} == *$front* ]] && [[ ${table_name} == *$back ]];then start=`echo ${table_name#*$front} | awk -F $back '{print $1}' | awk -F '-' '{print $1}'`;end=`echo ${table_name#*$front} | awk -F $back '{print $1}' | awk -F '-' '{print $2}'`;table_name=${table_name%$front*};fi
sink_db=`[[ $(echo $config |jq '.sink_db') = null ]] && echo $default_sink_db || echo $(echo $config |jq '.sink_db' | awk -F '"' '{print $2}')`
split_pk=`[[ $(echo $config |jq '.split_pk') = null ]] && echo $default_split_pk || echo $(echo $config |jq '.split_pk' | awk -F '"' '{print $2}')`
channel=`[[ $(echo $config |jq '.channel') = null ]] && echo $default_channel || echo $(echo $config |jq '.channel' | awk -F '"' '{print $2}')`
is_snapshot=$(echo $config |jq '.is_snapshot' | awk -F '"' '{print $2}')
createTablesql="CREATE TABLE IF NOT EXISTS $sink_db.$sink_table ("
#根据配置类型读取数据源元数据信息
case $db_type in
  "mysql")
      meta_sql="select COLUMN_NAME,DATA_TYPE,replace(COLUMN_COMMENT,' ','') from information_schema.COLUMNS where TABLE_NAME='$table_name$start' and table_schema='$db_name' `[[ $(echo $config |jq '.exclude_column') != null ]] && echo " and COLUMN_NAME not in ($(echo $config |jq '.exclude_column' | awk -F '"' '{print $2}'))"`"
      for e in `mysql -h $hosts -P $port -u $user_name -p''''$password'''' -s -e "$meta_sql" | awk -F ' ' '{print $1"="$2"="$3}'`;do k=`echo $e | awk -F '=' '{print $1}'`;t=`echo $e | awk -F '=' '{print $2}'`; v=`echo $e | awk -F '=' '{print $3}'`;if [[ ${sign}x == x ]];then createTablesql=$createTablesql'`'$k'`'" varchar(2000) DEFAULT '' COMMENT '${v//\'/}',";sign="1";else createTablesql=$createTablesql'`'$k'`'" STRING DEFAULT '' COMMENT '${v//\'/}',";fi;if [[ `is_transform_type $t` = STRING ]];then words=$words"replace(replace(\`$k\`,char(37),'|||'),unhex('C2A0'),''),"; else words=$words"\`$k\`",;fi;column=$column'"'$k'",' ;done;
      comment=`mysql -h $hosts -P $port -u $user_name -p''''$password'''' -s -e "select TABLE_COMMENT from information_schema.TABLES where table_name ='$table_name$start'" | awk -F ' ' '{print $1}'`
      reader_type="mysqlreader";
      jdbc_url="jdbc:mysql://$hosts:$port";
      words=$words`[[ $(echo $config |jq '.partition_column')x != nullx ]] && echo "\\\`$(echo $config | jq '.partition_column' | awk -F '"' '{print $2}')\\\` as \\\`dt\\\`,"`
      words=$words`[[ $is_snapshot == 1 ]] && echo "'$yesterday' as \\\`dt\\\`,"`
      if [[ ${start}x == x ]] && [[ ${end}x == x ]]; then
      	if [[ $split_pk == $default_split_pk ]]; then
		query_sql='"'"SELECT ${words%?} from \`${db_name}\`.\`${table_name}\` where 1=1"`[[ $(echo $config |jq '.partition_column') != null ]] && echo " and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') >='$start_dt' and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') <'$next_day'"`'",';
	else
	   for i in $(seq 0 $(( $channel - 1 )));do
		query_sql=$query_sql'"'"SELECT ${words%?} from \`${db_name}\`.\`${table_name}\` where MOD(CRC32($split_pk),$channel) =$i"`[[ $(echo $config |jq '.partition_column') != null ]] && echo " and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') >='$start_dt' and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') <'$next_day'"`'",';
           done;
	fi
      else
          for i in $(seq -w $start $end);do
		query_sql=$query_sql'"'"SELECT ${words}$i from \`${db_name}\`.\`${table_name}$i\` where 1=1"`[[ $(echo $config |jq '.partition_column') != null ]] && echo " and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') >='$start_dt' and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') <'$next_day'"`'",';
	  done;
      createTablesql=$createTablesql'`'"table_num"'`'" INT COMMENT '分表后缀',"
      fi
      ;;
  "sqlserver")
      meta_sql="SELECT B.name AS column_name, C.value AS column_description, T.name FROM sys.tables A INNER JOIN sys.columns B ON B.object_id = A.object_id LEFT JOIN sys.extended_properties C ON C.major_id = B.object_id AND C.minor_id = B.column_id LEFT JOIN sys.types T ON B.system_type_id=T.system_type_id WHERE A.name = '${table_name}' `[[ $(echo $config |jq '.exclude_column') != null ]] && echo " and B.name not in ($(echo $config |jq '.exclude_column' | awk -F '"' '{print $2}'))"`"
      for e in `sqlcmd -S $hosts,$port -U $user_name -P''''$password'''' -d$db_name  -y 1024 -Q "$meta_sql" | awk 'NR>4{print p2}{p2=p;p=$0}' | awk -F ' ' '{print $1"="$2"="$3}'`;do k=`echo $e | awk -F '=' '{print $1}'`; v=`echo $e | awk -F '=' '{print $2}'`;t=`echo $e | awk -F '=' '{print $3}'`;if [[ ${sign}x == x ]];then createTablesql=$createTablesql'`'$k'`'" varchar(2000) DEFAULT '' COMMENT '${v//\'/}',";sign="1";else createTablesql=$createTablesql'`'$k'`'" STRING DEFAULT '' COMMENT '${v//\'/}',";fi; if [[ `is_transform_type $t` = STRING ]];then words=$words"rtrim(ltrim(replace(replace([$k],CHAR(37),'|||'),CHAR(13),''))),";else words=$words"[$k],";fi;column=$column'"'$k'",' ;done;
      comment=`sqlcmd -S $hosts -U $user_name -P''''$password'''' -d$db_name  -y 1024 -Q "select c.value from sys.tables a left join sys.extended_properties c on c.major_id = a.object_id and minor_id=0 where a.name ='${table_name}'" | awk 'NR>4{print p2}{p2=p;p=$0}' | awk -F ' ' '{print $1}'`
      reader_type="sqlserverreader"
      words=$words`[[ $(echo $config |jq '.partition_column')x != nullx ]] && echo "$(echo $config | jq '.partition_column' | awk -F '"' '{print $2}') as [dt],"`
      words=$words`[[ $is_snapshot == 1 ]] && echo "'$yesterday' as [dt],"`
      jdbc_url="jdbc:sqlserver://$hosts:$port";
      query_sql='"'"SELECT ${words%?} from [${db_name}].[dbo].[${table_name}] where 1=1"`[[ $(echo $config |jq '.partition_column') != null ]] && echo " and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') >='$start_dt' and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') <'$next_day'"`'",';
	  ;;
  "postgresql")
meta_sql="select a.attname, t.typname,null from pg_class c left join pg_catalog.pg_namespace n on c.relnamespace = n.oid, pg_attribute a left join pg_description b on a.attrelid = b.objoid and a.attnum = b.objsubid, pg_type t where c.relname = '${table_name}' and a.attnum > 0 and a.attrelid = c.oid and a.atttypid = t.oid and n.nspname = 'public'"`[[ $(echo $config |jq '.exclude_column') != null ]] && echo " and a.attname not in ($(echo $config |jq '.exclude_column' | awk -F '"' '{print $2}'))"`" order by a.attnum"
       for e in `PGPASSWORD=''''$password'''' psql -h $hosts -p $port -U $user_name -w  -d $db_name -c "$meta_sql" | awk 'NR>4{print p2}{p2=p;p=$0}' | awk -F '|' '{print $1" "$2" "$3}' | awk -F ' ' '{print $1"="$2"="$3}'`;do k=`echo $e | awk -F '=' '{print $1}'`; t=`echo $e | awk -F '=' '{print $2}'`;v=`echo $e | awk -F '=' '{print $3}'`;if [[ ${sign}x == x ]];then createTablesql=$createTablesql'`'$k'`'" varchar(2000) DEFAULT '' COMMENT '${v//\'/}',";sign="1";else createTablesql=$createTablesql'`'$k'`'" STRING DEFAULT '' COMMENT '${v//\'/}',";fi; if [[ `is_transform_type $t` = STRING ]];then words=$words"replace(replace(cast($k as text),CHR(37),'|||'),CHR(13),''),";else words=$words"$k,";fi;column=$column'"'$k'",' ;done;
      comment=`PGPASSWORD=''''$password'''' psql -h $hosts -p $port -U $user_name -w  -d $db_name -c "select cast(obj_description(relfilenode,'pg_class') as varchar) as comment from pg_class c where  relkind = 'r' and relname ='${table_name}'" | awk 'NR>4{print p2}{p2=p;p=$0}'`
      reader_type="postgresqlreader"
      jdbc_url="jdbc:postgresql://$hosts:$port/$db_name";
      words=$words`[[ $(echo $config |jq '.partition_column')x != nullx ]] && echo "cast($(echo $config | jq '.partition_column' | awk -F '"' '{print $2}') as date) as dt,"`
      words=$words`[[ $is_snapshot == 1 ]] && echo "'$yesterday' as dt,"`
      query_sql='"'"SELECT ${words%?} from ${table_name} where 1=1"`[[ $(echo $config |jq '.partition_column') != null ]] && echo " and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') >='$start_dt' and $(echo $config |jq '.partition_column' | awk -F '"' '{print $2}') <'$next_day'"`'",';
	  ;;
    *)
      echo "ERROR: sorry, type $db_type not support !!!"
      exit 4
      ;;
esac
column=$column`[[ $(echo $config |jq '.partition_column')x != nullx || $is_snapshot == 1 ]] && echo '"'dt'",'`
column=$column`[[ ${start}x == x ]] && [[ ${end}x == x ]] || echo '"'table_num'",'`
comment="COMMENT "`[[ $(echo $config |jq '.comment') = null ]] && echo "'$comment'" || echo "'"$(echo $config |jq '.comment'| awk -F '"' '{print $2}')"'"`
createTablesql=${createTablesql%?}`[[ $(echo $config |jq '.partition_column')x != nullx || $is_snapshot == 1 ]] && echo ",\\\`dt\\\` DATE NOT NULL DEFAULT '1970-01-01' COMMENT '分区字段') $comment PARTITION BY RANGE ( \\\`dt\\\` ) ( PARTITION p_19700101 VALUES LESS THAN ( '2022-01-01' ), FROM ( '2022-01-01' ) TO ( '$yesterday' ) INTERVAL 1 DAY ) DISTRIBUTED BY RANDOM BUCKETS 1 PROPERTIES ( 'dynamic_partition.enable' = 'true', 'dynamic_partition.time_unit' = 'DAY', 'dynamic_partition.end' = '1', 'dynamic_partition.prefix' = 'p_', 'dynamic_partition.create_history_partition' = 'true', 'dynamic_partition.history_partition_num' = '1', 'dynamic_partition.buckets' = '1' );" || echo ") $comment  DISTRIBUTED BY RANDOM BUCKETS 1;"`

create_sql_path=$temp_shell_file_dir/$taskname"_create"$temp_time.sql
file_path_=$temp_shell_file_dir/$taskname"__"$temp_time.json
file_path_sed=$temp_shell_file_dir/$taskname"_"$temp_time.sed
file_path=$temp_shell_file_dir/$taskname"_"$temp_time.json
sed "s/schema_words/${column%?}/g" $DATAX_HOME/job/doris_core.json > $file_path_
echo "s/query_sql/${query_sql%?}/g" > $file_path_sed
sed -f $file_path_sed $file_path_ >$file_path

if [[ ${initial_flag} == 1 ]];then initdate=$startdate;fi
#需要判断是否分区
if [[ $(echo $config |jq '.partition_column')x != nullx || $is_snapshot == 1 ]];then
   truncate_sql="ALTER TABLE $sink_db.$sink_table set ('dynamic_partition.enable' = 'false');"
   if [[ "$(date -d "$initdate" +%s)" -le "$(date -d "2022-01-01" +%s)" ]];then initdate="2022-01-01";fi
   while [ "$(date -d "$initdate" +%s)" -le "$(date -d "$startdate" +%s)" ]
   do
     temp_next_day=$(date -d "$initdate + 1 day" +%Y-%m-%d)
     truncate_sql="${truncate_sql}ALTER TABLE $sink_db.$sink_table DROP PARTITION IF EXISTS p_$(date -d $initdate +%Y%m%d);ALTER TABLE $sink_db.$sink_table ADD PARTITION IF NOT EXISTS p_$(date -d $initdate +%Y%m%d) VALUES [('$initdate'),('$temp_next_day'));"
     initdate=$(date -d "$initdate + 1 day" +%Y-%m-%d)
   done;
   createTablesql=$createTablesql${truncate_sql}"ALTER TABLE $sink_db.$sink_table set ('dynamic_partition.enable' = 'true');"
else
   createTablesql="DROP TABLE IF EXISTS $sink_db.$sink_table;"$createTablesql
fi
echo "生成的脚步路径:"$file_path
echo $createTablesql > $create_sql_path
echo "sql脚本路径:$create_sql_path"
mysql --login-path=doris -e "source $create_sql_path" &&
python ${DATAX_HOME}/bin/datax.py -p "-Dreader_type=${reader_type} -Duser_name=${user_name} -Dpassword='${password}' -Djdbc_url=${jdbc_url} -Dsink_db=${sink_db} -Dsink_table=${sink_table} -Dchannel=${channel}" $file_path
if [[ $? -eq 0 ]];then if [[ ${initial_flag} == 0 ]];then update_init_sql="update scheduler_meta.table_info set initial_flag = 1 where task_name = '$taskname';";mysql --login-path=scheduler_meta -e "$update_init_sql";fi;else exit 4;fi
}

get_transaction() {
 label_name=$1
 result=$(mysql --login-path=doris --raw -e "show transaction from $profix where label='$label_name'")
  while read -r f1 f2 f3 f4 f5 f6; do transaction_status="$f5";done <<< "$result"
  if [[ ${transaction_status} != "VISIBLE" ]];then
    retry_count=3
    while [ $retry_count -gt 0 ]
       do
          retry_count=$((retry_count - 1))
          sleep 5s
          result=$(mysql --login-path=doris --raw -e "show transaction from $profix where label='$label_name'")
          while read -r f1 f2 f3 f4 f5 f6; do transaction_status="$f5";done <<< "$result"
          if [[ ${transaction_status} == "VISIBLE" || $retry_count -lt 1 ]];then
            break
          fi
    done
  fi
  echo $transaction_status
}

function runTransform(){
if [[ ${initdate}x == x ]];then initdate=$startdate;fi
result=`mysql --login-path=scheduler_meta -e "SELECT cnf_value from scheduler_meta.common_config where cnf_key=1" | awk 'NR==2{print}'`
if [[ $(echo $result |jq '.flag') == 1 ]];then
    initdate=$(echo $result |jq '.initdate' | awk -F '"' '{print $2}');
    if [[ $(echo $result |jq '.dt')x != nullx ]];then startdate=$(echo $result |jq '.dt' | awk -F '"' '{print $2}');fi
fi
#start_dt=$initdate
start_dt=`[[ "$(date -d "$initdate" +%s)" -lt "$(date -d "$min_date" +%s)" ]] && echo $min_date || echo $initdate`
#min_next_day=`date -d "$min_date +1 day" +%Y-%m-%d`
#replace_dt=`[[ "$(date -d "$startdate" +%s)" -lt "$(date -d "$min_date" +%s)" ]] && echo $min_next_day || echo $startdate`
file_path=$temp_shell_file_dir/$taskname"_"$temp_time.sql
sql_file=$SQL_PATH/$taskname.sql
if [ ! -f "$sql_file" ]; then
  echo "$sql_file文件未找到，请检查!!"
  exit 4
fi
start_sql=""
replace_sql=""
end_sql=""
recover_sql=""
if grep -Eqi "TRUNCATE TABLE.*PARTITION.*" $SQL_PATH/$taskname.sql; then
    replace_sql="ALTER TABLE $profix.$taskname set ('dynamic_partition.enable' = 'false');"
    recover_sql=$recover_sql$replace_sql
    while [ "$(date -d "$start_dt" +%s)" -le "$(date -d "$startdate" +%s)" ]
    do
      p_date=p_$(date -d $start_dt +%Y%m%d)
      p_next_day=$(date -d $start_dt+1day +%Y-%m-%d)
      replace_sql=${replace_sql}"ALTER TABLE $profix.$taskname DROP PARTITION IF EXISTS $p_date;ALTER TABLE $profix.$taskname ADD PARTITION IF NOT EXISTS $p_date  VALUES [('$start_dt'),('$p_next_day'));"
      recover_sql=$recover_sql"ALTER TABLE $profix.$taskname DROP PARTITION IF EXISTS $p_date FORCE;RECOVER PARTITION $p_date FROM $profix.$taskname;"
      start_dt=$p_next_day
    done;
    if [[ "$(date -d "$initdate" +%s)" -lt "$(date -d "$min_date" +%s)" ]];then replace_sql=${replace_sql}"ALTER TABLE $profix.$taskname DROP PARTITION IF EXISTS p_19700101;ALTER TABLE $profix.$taskname ADD PARTITION IF NOT EXISTS p_19700101 VALUES LESS THAN ('2023-06-01');";recover_sql=$recover_sql"ALTER TABLE $profix.$taskname DROP PARTITION IF EXISTS p_19700101 FORCE;RECOVER PARTITION p_19700101 FROM $profix.$taskname;";fi
    end_sql=";ALTER TABLE $profix.$taskname set ('dynamic_partition.enable' = 'true');"
    recover_sql=$recover_sql$end_sql
elif grep -Eqi "TRUNCATE TABLE.*" $SQL_PATH/$taskname.sql; then
   start_sql="DROP TABLE IF EXISTS $profix.$taskname;"
   recover_sql="DROP TABLE IF EXISTS $profix.$taskname FORCE;RECOVER TABLE $profix.$taskname;"
fi
sed "s/^TRUNCATE TABLE.*;/${replace_sql}/I" $SQL_PATH/$taskname.sql > $file_path
sed -i "1s/^/$start_sql\n/" $file_path
sed -i "s/@dt/'${startdate}'/g" $file_path &&
sed -i "s/@start_dt/'${initdate}'/g" $file_path &&
sed -i "s/@label/$label_name/g" $file_path &&
#sed -i "s/INSERT.*INTO.*$profix.*$taskname[ |\`]*(/INSERT INTO $profix.$taskname with label $label_name (/I" $file_path &&
echo $end_sql >> $file_path
echo "执行脚本路径是:"$file_path
if grep -Eqi -e "-- process_name:default" "$file_path" || ! grep -Eqi -e "-- process_name:[^ ]+" "$file_path"; then mysql --login-path=scheduler_meta -e "INSERT INTO scheduler_meta.recover_info (dt,table_name,recover_sql) values (\"$yesterday\",\"$taskname\",\"$recover_sql\")";fi
mysql --login-path=doris -e "source $file_path"
if [[ $? == 0 && $(get_transaction $label_name) == "VISIBLE" ]];then
  echo "task sucessed !!!"
else
  exit 4;
fi
}

function add_quotes_and_commas {
  local input="$1"
  local output=""

  IFS=',' read -ra fields <<< "$input"
  for field in "${fields[@]}"; do
    output="$output,\"$field\""
  done

  # 去掉开头多余的逗号
  output="${output#,}"

  echo "$output"
}
function exportTask(){
realname=${taskname#*_}
result=$(mysql --login-path=scheduler_meta -e "SELECT d.hosts,d.port,d.user_name,d.password,d.db_type,t.db_name,t.table_name,t.initial_flag,t.config from scheduler_meta.table_info t left join scheduler_meta.db_info d on t.server_name =d.server_name where t.task_name =trim('$taskname')")
if [[ ${result}x == x ]];then
echo "ERROR: taskname not configured"
exit 4
fi
while read -r f1 f2 f3 f4 f5 f6 f7 f8 f9; do hosts="$f1" port="$f2" user_name="$f3" password="$f4" db_type="$f5" db_name="$f6" table_name="$f7" initial_flag="$f8" config="$f9";done <<< "$result"
if [[ ${config}x == x ]];then config="{}";fi
if [[ ${config}x == NULLx ]];then config="{}";fi
if [[ $(jq -r '.validator_sql' <<< "$config" 2>/dev/null) != null && $(mysql --login-path=doris -e "$(jq -r '.validator_sql' <<< "$config" 2>/dev/null)" | awk 'NR==2{print}') == 0 ]];then
echo "校验不通过!!!"
exit 0
fi
pre_sql=`[[ $(echo $config |jq '.pre_sql') = null ]] && echo 'select 1' || echo $(echo $config |jq '.pre_sql' | awk -F '"' '{print $2}')`
post_sql=`[[ $(echo $config |jq '.post_sql') = null ]] && echo 'select 1' || echo $(echo $config |jq '.post_sql' | awk -F '"' '{print $2}')`
query_sql=`[[ $(echo $config |jq '.query_sql') = null ]] && echo 'select 1' || echo $(echo $config |jq '.query_sql' | awk -F '"' '{print $2}')`
columns=`[[ $(echo $config |jq '.columns') = null ]] && echo '' || echo $(echo $config |jq '.columns' | awk -F '"' '{print $2}')`
case $db_type in
  "mysql")
      write_type="mysqlwriter";
      jdbc_url="jdbc:mysql://$hosts:$port/$db_name";
      ;;
    *)
      echo "ERROR: sorry, type $db_type not support !!!"
      exit 4
      ;;
esac
schema_words=$(add_quotes_and_commas "$columns")
file_path=$temp_shell_file_dir/$taskname"_"$temp_time.json
sed "s/schema_words/${schema_words}/g" $DATAX_HOME/job/export_core.json > $file_path
sed -i "s~query_sql~${query_sql}~g" $file_path
sed -i "s/pre_sql/${pre_sql}/g" $file_path
sed -i "s/post_sql/${post_sql}/g" $file_path
echo "执行任务的路径是:"$file_path
python ${DATAX_HOME}/bin/datax.py -p "-Dwrite_type=${write_type} -Duser_name=${user_name} -Dpassword='${password}' -Dsink_table=${table_name} -Djdbc_url=${jdbc_url} -Dstartdate=${startdate}" $file_path
}

function schedule(){
while true; do
if [ "$(date "+%H%M%S")" -gt "$(echo $startdate | tr -d ':')" ]; then
        echo "校验通过!!!";
        break
else
        echo "等待校验!!!";
        sleep 5s
fi
done
}

function validatorTask(){
result=$(mysql --login-path=scheduler_meta -e "SELECT d.hosts,d.port,d.user_name,d.password,d.db_type,t.db_name,t.table_name,t.initial_flag,t.config from scheduler_meta.table_info t left join scheduler_meta.db_info d on t.server_name =d.server_name where t.task_name =trim('$taskname')")
if [[ ${result}x == x ]];then
echo "ERROR: taskname not configured"
exit 4
fi
while read -r f1 f2 f3 f4 f5 f6 f7 f8 f9; do hosts="$f1" port="$f2" user_name="$f3" password="$f4" db_type="$f5" db_name="$f6" table_name="$f7" initial_flag="$f8" config="$f9";done <<< "$result"

if [[ ${config}x == x ]];then config="{}";fi
if [[ ${config}x == NULLx ]];then config="{}";fi
validator_sql=`[[ $(echo $config |jq '.validator_sql') = null ]] && echo "select 1" || echo $(echo $config |jq '.validator_sql' | awk -F '"' '{print $2}')`
while true; do
 result=$(mysql -h $hosts -P $port -u $user_name -p''''$password''''  -D $db_name -s -e "$validator_sql")
  if [[ "$startdate" == "$result" ]]; then
  	echo "日期相同" ${result}
	break
  else
    echo "日期不相同" ${result}
    sleep 5m
  fi
done
}

function specialTask(){
file_path=$temp_shell_file_dir/$taskname"_"$temp_time.sql
sql_file=$SQL_PATH/$taskname.sql

if [ ! -f "$sql_file" ]; then
  echo "$sql_file文件未找到，请检查!!"
  exit 4
fi
retry_count=3
start_dt=$initdate
final_result=-1
echo "执行任务的路径是:$file_path"
if [[ ${taskname}x == dws_alct_expected_future_sales_dfx ]];then  mysql --login-path=doris -e "truncate table dws.dws_alct_expected_future_sales_df;"; fi;
if [[ ${taskname}x == dws_pcct_purchase_return_supply_days_details_dfx ]];then  mysql --login-path=doris -e "truncate table dws.dws_pcct_purchase_return_supply_days_details_df;"; fi;
if [[ ${taskname}x == dws_mk_spring_expected_future_sales_dfx ]];then  mysql --login-path=doris -e "truncate table dws.dws_mk_spring_expected_future_sales_df;"; fi;
 while [ "$(date -d "$initdate" +%s)" -le "$(date -d "$startdate" +%s)" ]
 do
   label_name=${taskname}_$(uuidgen | tr -d "-")
   cp $sql_file $file_path
   echo "当前执行的日期是:$initdate"
   sed -i "s/@dt/'${initdate}'/g" $file_path
   sed -i "s/@start_dt/'${start_dt}'/g" $file_path
   sed -i "s/@label/$label_name/g" $file_path
   mysql --login-path=doris -e "source $file_path"
   if [[ $? -eq 0 && $(get_transaction $label_name) == "VISIBLE" ]];then
     final_result=0
   else
       while [ $retry_count -gt 0 ]
       do
          retry_count=$((retry_count - 1))
          sleep 5m
          echo "第$((3 - retry_count)) 次执行"
          mysql --login-path=doris -e "source $file_path"
          curr_result=$?
          if [[ $curr_result -ne 0 && $retry_count -lt 1 ]];then
            final_result=1
            exit 4
          elif [[ $curr_result -eq 0 && $(get_transaction $label_name) == "VISIBLE" ]];then
            final_result=0
            retry_count=3
            break
          fi
       done
   fi
   initdate=$(date -d "$initdate + 1 day" +%Y-%m-%d)
 done;
   if [[ $final_result -ne 0 ]];then exit 4;fi
}

function get_index() {
IFS=$3 read -ra arrary <<< "$1"
# 使用循环和条件判断来找到元素的角标
result_index=99
index=0
for e in "${arrary[@]}"; do
  if [[ "$e" == "$2" ]]; then
    result_index=$index
    break
  fi
  index=$((index + 1))
done
echo $result_index
}

function httpTask(){
result=$(mysql --login-path=scheduler_meta -e "SELECT d.hosts,d.password,t.config from scheduler_meta.table_info t left join scheduler_meta.db_info d on t.server_name =d.server_name where t.task_name =trim('$taskname')")
if [[ ${result}x == x ]];then
echo "ERROR: taskname not configured"
exit 4
fi
while read -r f1 f2 f3; do hosts="$f1" password="$f2" config="$f3";done <<< "$result"
if [[ ${config}x == x ]];then config="{}";fi
if [[ ${config}x == NULLx ]];then config="{}";fi
local app_id=$(echo $config | jq -r '.app_id')
local entry_id=$(echo $config | jq -r '.entry_id')
local primary_key=$(echo $config | jq -r '.primary_key')
local query_sql=$(echo $config | jq -r '.query_sql')
local query_sql="${query_sql/'${startdate}'/$startdate}"
local http_result=$(mysql --login-path=doris -e "$query_sql" | sed 's/\t/^/g')
if [[ ${http_result}x == x ]];then
echo "no data!!!"
exit 0
fi
local all_key=`echo $http_result | cut -d' ' -f1`
IFS=$separator_column read -ra arr_all_key <<< "$all_key"
IFS=$separator_comma read -ra arr_primary_key <<< "$primary_key"
local master_index=""
local others_index=""
for e in "${arr_all_key[@]}"; do
        if echo "$primary_key" | grep -q "$e," || echo "$primary_key" | grep -q ",$e"; then
                local master_index=$master_index$(get_index $all_key $e $separator_column)$separator_column;
        else
                local others_index=$others_index$(get_index $all_key $e $separator_column)$separator_column;
        fi
done
local master_index=${master_index%?}
local others_index=${others_index%?}
IFS=$separator_column read -ra arr_master_index <<< "$master_index"
IFS=$separator_column read -ra arr_others_index <<< "$others_index"
while read -r line;do
        IFS=$separator_column read -ra arr_line <<< "$line"
        local key=""
        local value="{"
        for e in "${arr_master_index[@]}"; do
                local key=$key${arr_line[$e]}$separator_column
        done
        for e in "${arr_others_index[@]}"; do
                local value=$value'"'${arr_all_key[$e]}'":{"value":"'${arr_line[$e]}'"},'
        done
        local key=${key%?}
        local value=${value%?}'}'
        if [[ -n "${map_http[$key]}" ]]; then
                map_http[$key]="${map_http[$key]},$value"
        else
                map_http[$key]="$value"
        fi
done <<< "${http_result#*$'\n'}";
IFS=$separator_line
for k in "${!map_http[@]}"; do
        local base_json="{\"app_id\":\"$app_id\",\"entry_id\":\"$entry_id\",\"is_start_workflow\":true,\"is_start_trigger\":true,\"data\":{\"detail_info\":{\"value\":[]}}}"
        IFS=$separator_column read -ra arr_k <<< "$k"
        for primary_e in "${arr_primary_key[@]}"; do
                local base_json=$(echo "$base_json" | jq --arg json_k "$primary_e" --arg json_v "${arr_k[$(get_index $primary_key $primary_e $separator_comma)]}" '.data[$json_k].value = $json_v' <<< "$base_json")
        done
        local values=(${map_http[$k]})
        local json_array_str="[${values[*]}]"
        local base_json=$(jq --arg supplierName "$supplier_name" --argjson jsonArray "$json_array_str" '.data.detail_info.value = $jsonArray' <<< "$base_json")
curl --location $hosts --header 'Content-Type: application/json'  --header 'Authorization: Bearer '$password --data "$base_json"
sleep 5s
done
}

if [[ ${profix}x == odsx ]];then
  if [[ $taskname == *of || $taskname == *nf ]]; then
        runTransform
  else
        runOds
  fi
elif [[ ${profix}x == exportx ]];then
	exportTask
elif [[ ${profix}x == validatorx ]];then
        validatorTask
elif [[ ${profix}x == schedulex ]];then
	schedule
elif [[ ${profix}x == httpx ]];then
        httpTask
else
  if [[ ${taskname}x == dws_ivct_stock_health_assessment_dix  || ${taskname}x == dws_alct_expected_future_sales_dfx  || ${taskname}x == dws_pcct_purchase_return_supply_days_details_dfx || ${taskname}x == dws_ivct_stock_health_assessment_country_dsx || ${taskname}x == dws_mk_spring_expected_future_sales_dfx || ${taskname}x == dws_ivct_sku_site_future_oversea_stock_dsx || ${taskname}x == dws_ivct_sku_ship_country_future_oversea_stock_dsx ]];then
        specialTask
  else
        runTransform
  fi
fi
