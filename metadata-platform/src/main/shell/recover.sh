#!/bin/bash
yesterday=`date -d yesterday +%Y-%m-%d`
echo "恢复数仓数据快照时间为:$yesterday"
result=$(mysql --login-path=scheduler_meta -e "SELECT id,table_name,recover_sql from scheduler_meta.recover_info WHERE dt='$yesterday' and recover_sql !='' order by id;" | awk 'NR>1')
while read -r f1 f2 f3;
 do
   id=$f1;table_name=$f2;recover_sql=$f3;
   echo "开始恢复id是:$id,表名是:$table_name"
   $(mysql --login-path=doris -e "$recover_sql")
   if [[ $? == 0 ]];then echo "恢复结果是:成功!!!" ;else echo "恢复结果是:失败!!!";exit 4;fi
done <<< "$result"