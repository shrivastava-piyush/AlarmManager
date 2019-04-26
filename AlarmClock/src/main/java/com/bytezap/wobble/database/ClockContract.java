package com.bytezap.wobble.database;

import android.provider.BaseColumns;

final class ClockContract {

	public ClockContract() {}

	public interface Alarm extends BaseColumns {

		String ALARM_TABLE_NAME = "alarms";
		String ALARM_NAME = "name";
		String ALARM_VOCAL_MESSAGE = "vocal_message";
		String VOCAL_MESSAGE_TYPE = "vocal_message_type";
		String VOCAL_MESSAGE_PLACE = "vocal_message_place";
		String ALARM_TIME_HOUR = "hour";
		String ALARM_TIME_MINUTE = "minutes";
		String ALARM_REPEAT_DAYS = "days";
		String ALARM_TONE = "tone";
		String ALARM_URI_IDS = "uri_ids";
		String ALARM_TONE_TYPE = "tone_type";
		String ALARM_VIBRATION = "vibration";
		String ALARM_ENABLED = "enabled";
		String ALARM_DISMISS_METHOD = "dismiss_method";
		String ALARM_DISMISS_LEVEL = "dismiss_level";
		String ALARM_SNOOZE_PROBLEM = "snooze_problem";
		String ALARM_SNOOZE_LEVEL = "snooze_level";
		String ALARM_DISMISS_SKIP = "dismiss_skip";
		String ALARM_SNOOZE_SKIP = "snooze_skip";
		String ALARM_MATH_DISMISS_NUMBER = "dismiss_number";
		String ALARM_MATH_SNOOZE_NUMBER = "snooze_number";
		String ALARM_SNOOZE_TIME_INDEX = "snooze_time_index";
		String ALARM_DISMISS_SHAKE = "dismiss_shake";
		String ALARM_SNOOZE_SHAKE = "snooze_shake";
        String ALARM_CHECK = "wakeup_check";
		String ALARM_IS_LAUNCH_APP = "launch_app";
		String ALARM_LAUNCH_APP_PACKAGE = "launch_app_package";
		String ALARM_BARCODE_TEXT = "barcode_text";
		String ALARM_IMAGE_PATH = "image_path";
		String ALARM_IS_SKIPPED = "skipped";
	}

	interface Instance extends BaseColumns {

		String INSTANCE_TABLE_NAME = "instances";
		String INSTANCE_ID = "ins_id";
		String INSTANCE_NAME = "ins_name";
		String INSTANCE_VOCAL_MESSAGE = "ins_vocal_message";
		String INSTANCE_VOCAL_MESSAGE_TYPE = "ins_vocal_message_type";
		String INSTANCE_VOCAL_MESSAGE_PLACE = "ins_vocal_message_place";
		String INSTANCE_TIME_HOUR = "ins_hour";
		String INSTANCE_TIME_MINUTE = "ins_minutes";
		String INSTANCE_TIME_DATE = "ins_date";
		String INSTANCE_TIME_MONTH = "ins_month";
		String INSTANCE_TIME_YEAR = "ins_year";
		String INSTANCE_REPEAT_DAYS = "ins_days";
		String INSTANCE_TONE = "ins_tone";
		String INSTANCE_URI_IDS = "ins_uri_ids";
		String INSTANCE_TONE_TYPE = "ins_tone_type";
		String INSTANCE_VIBRATION = "ins_vibration";
		String INSTANCE_DISMISS_METHOD = "ins_dismiss_method";
		String INSTANCE_DISMISS_LEVEL = "ins_dismiss_level";
		String INSTANCE_SNOOZE_PROBLEM = "ins_snooze_problem";
		String INSTANCE_SNOOZE_LEVEL = "ins_snooze_level";
		String INSTANCE_DISMISS_SKIP = "ins_dismiss_skip";
		String INSTANCE_SNOOZE_SKIP = "ins_snooze_skip";
		String INSTANCE_MATH_DISMISS_NUMBER = "ins_dismiss_number";
		String INSTANCE_MATH_SNOOZE_NUMBER = "ins_snooze_number";
		String INSTANCE_SNOOZE_TIMES = "ins_snooze_times";
		String INSTANCE_SNOOZE_TIME_INDEX = "ins_snooze_time_index";
		String INSTANCE_DISMISS_SHAKE = "ins_dismiss_shake";
		String INSTANCE_SNOOZE_SHAKE = "ins_snooze_shake";
        String INSTANCE_CHECK = "ins_wakeup_check";
		String INSTANCE_IS_LAUNCH_APP = "ins_launch_app";
		String INSTANCE_LAUNCH_APP_PACKAGE = "ins_launch_app_package";
		String INSTANCE_BARCODE_TEXT = "ins_barcode_text";
		String INSTANCE_IMAGE_PATH = "ins_image_path";
        String INSTANCE_STATE = "ins_alarm_state";
	}

	interface Preset extends BaseColumns{

		String PRESET_TABLE_NAME = "presets";
		String PRESET_NAME = "name";
		String PRESET_HOURS= "hours";
		String PRESET_MINUTES= "minutes";
		String PRESET_SECONDS= "seconds";
	}

}
