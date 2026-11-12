# 城市数据来源

- 提供方：彩云天气。
- 原始文件：https://docs.caiyunapp.com/weather-api/20240617-adcode.csv
- 数据版本：2024-06-17；本项目导入日期：2026-11-11。
- 本地文件：`cities.tsv`，UTF-8，无表头；字段为 `adcode、简称、完整地址、经度、纬度、简称拼音音节、地址拼音音节`，以 Tab 分隔。前五列保持彩云导入值，后两列由本项目生成，音节用空格分隔。
- 导入后 3,184 条，范围为中国大陆城市／区县，不含省级汇总和港澳台。行政区划可能已变更；支持中文、全拼及首字母的连续关键词匹配，不支持全球地点、街道地址或任意错拼纠正。
- 过滤规则：行政代码必须为数字；省级前缀小于 71；排除以 0000 结尾的省级记录；校验地址及坐标；按行政代码去重。
- 简称规则：从完整地址中去除已识别的省／自治区前缀，再去除上级市前缀（若仍有下级名称）。仅当去除上级行政区后仍有剩余名称时才执行该步，自治州／盟／地区本身保留完整名称。直辖市保留“北京市”等完整名称。搜索结果始终同时显示原始完整地址。
- 深圳默认记录：440300，广东省深圳市，经度 114.057939，纬度 22.543527。

使用城市／区县中心点查询天气，不表示用户的设备位置。新增行政区或修订坐标需重新导入并复核同名地点、直辖市和默认深圳记录。

## 拼音索引（2026-11-11）

由 `scripts/generate_city_pinyin.py` 使用 [pypinyin 0.55.0](https://github.com/mozillazg/python-pinyin/tree/v0.55.0) 生成；该工具只在维护数据时运行，APK 不包含 Python 或拼音运行库。授权说明见 `PINYIN_LICENSE.txt`。普通 Gradle 构建直接使用已提交的七列数据，无需安装 Python 依赖。

```bash
python3 -m venv /tmp/sunnyweather-pinyin
/tmp/sunnyweather-pinyin/bin/pip install pypinyin==0.55.0
/tmp/sunnyweather-pinyin/bin/python scripts/generate_city_pinyin.py
/tmp/sunnyweather-pinyin/bin/python scripts/generate_city_pinyin.py --check
```

脚本保留前五列，统一生成简称和地址的无声调音节，并校验音节数量与有效性。`PLACE_READINGS` 固定常见多音地名读法，包括重庆、长沙、长治、单县、朝阳、六安、乐清等；不会枚举一个字的所有读音来增加错误命中。例如单县使用 `shan xian`，读音核对见[教育百科的单县词条](https://pedia.cloud.edu.tw/Entry/Detail?title=%E5%96%AE%E7%B8%A3)。通用词典并非完整地名库，未人工核对全部 3,184 条读音；发现问题时应修改词组规则、重新生成并补充检索用例。

2026-11-11 生成后全表为 405,534 字节，比原五列表增加 182,359 字节（压缩前）。原始城市 ID、名称、地址、经纬度逐行对比保持不变。运行时从音节派生全拼及首字母，不改变天气缓存中的 City 模型。
