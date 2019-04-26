package com.bytezap.wobble.test;

import android.media.RingtoneManager;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.bytezap.wobble.database.AlarmObject;
import com.bytezap.wobble.database.DataManager;

public class DBHelperTest extends AndroidTestCase {
	
	private DataManager dataManager;

	@Override
	protected void setUp() {
		super.setUp();
		
		RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");
        dataManager =  DataManager.getInstance(context);
	}
	
	@Override
	protected void tearDown() {
		super.tearDown();
	}
	
	private AlarmObject getModel() {
		AlarmObject model = new AlarmObject();
		model.name = "Test";
		model.hour = 6;
		model.minutes = 30;
		model.setRepeatingDay(AlarmObject.SUNDAY, false);
		model.setRepeatingDay(AlarmObject.MONDAY, true);
		model.setRepeatingDay(AlarmObject.WEDNESDAY, true);
		model.setRepeatingDay(AlarmObject.FRIDAY, true);
		model.setRepeatingDay(AlarmObject.SATURDAY, false);
		model.alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		return model;
	}
	
	public void testCreateAlarm() {
		AlarmObject model = getModel();

		long id = dataManager.createAlarm(model);

		AlarmObject returnModel = dataManager.getAlarmById(id);

		assertEquals(model.name, returnModel.name);
		assertEquals(model.hour, returnModel.hour);
		assertEquals(model.minutes, returnModel.minutes);
		assertEquals(model.alarmTone, returnModel.alarmTone);
		assertEquals(model.getRepeatingDay(AlarmObject.SUNDAY), returnModel.getRepeatingDay(AlarmObject.SUNDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.MONDAY), returnModel.getRepeatingDay(AlarmObject.MONDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.TUESDAY), returnModel.getRepeatingDay(AlarmObject.TUESDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.WEDNESDAY), returnModel.getRepeatingDay(AlarmObject.WEDNESDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.THURSDAY), returnModel.getRepeatingDay(AlarmObject.THURSDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.FRIDAY), returnModel.getRepeatingDay(AlarmObject.FRIDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.SATURDAY), returnModel.getRepeatingDay(AlarmObject.SATURDAY));
	}
	
	public void testUpdateAlarm() {
		AlarmObject model = getModel();
		model.name = "Update Test";
		model.hour = 22;
		model.minutes = 0;
		model.setRepeatingDay(AlarmObject.WEDNESDAY, false);

		long id = dataManager.createAlarm(model);

		AlarmObject returnModel = dataManager.getAlarmById(id);

		assertEquals(model.name, returnModel.name);
		assertEquals(model.hour, returnModel.hour);
		assertEquals(model.minutes, returnModel.minutes);
		assertEquals(model.alarmTone, returnModel.alarmTone);
		assertEquals(model.getRepeatingDay(AlarmObject.SUNDAY), returnModel.getRepeatingDay(AlarmObject.SUNDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.MONDAY), returnModel.getRepeatingDay(AlarmObject.MONDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.TUESDAY), returnModel.getRepeatingDay(AlarmObject.TUESDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.WEDNESDAY), returnModel.getRepeatingDay(AlarmObject.WEDNESDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.THURSDAY), returnModel.getRepeatingDay(AlarmObject.THURSDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.FRIDAY), returnModel.getRepeatingDay(AlarmObject.FRIDAY));
		assertEquals(model.getRepeatingDay(AlarmObject.SATURDAY), returnModel.getRepeatingDay(AlarmObject.SATURDAY));
	}
	
	public void testDeleteAlarm() {
		AlarmObject model = getModel();

		long id = dataManager.createAlarm(model);

		int rows = dataManager.deleteAlarmById(id);

		assertFalse(rows == 0);
		
		AlarmObject returnModel = dataManager.getAlarmById(id);
		
		assertNull(returnModel);
	}
	
}