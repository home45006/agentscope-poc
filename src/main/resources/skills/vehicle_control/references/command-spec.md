# 车控指令规范

## control_air_conditioner
action 合法值：
- set_temperature：设置温度
- set_fan：设置风速
- turn_on：开启空调
- turn_off：关闭空调

## control_window
position 合法值：front_left / front_right / rear_left / rear_right / all
action 合法值：open / close / set

## control_seat
target 合法值：driver / passenger / rear_left / rear_right
feature 合法值：heat / ventilate / position
action 合法值：on / off / level_1 / level_2 / level_3

## control_light
light_type 合法值：high_beam / low_beam / ambient / hazard
action 合法值：on / off

## control_door
action 合法值：lock / unlock
