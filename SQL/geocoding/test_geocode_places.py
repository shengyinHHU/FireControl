import csv, importlib.util, io, json, subprocess, tempfile, unittest
from argparse import Namespace
from pathlib import Path
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('geo',Path(__file__).with_name('geocode_places.py'))
g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)

def place(pid=''):
 return dict(place_id=pid,source_key='a'*64,name='测试机构',address='测试路1号',normalized_address='',place_type='elderly',record_key='a'*64)
def geo(level='门牌号',code='320116',coord='118.75,32.23'):
 return {'status':'1','geocodes':[{'location':coord,'adcode':code,'formatted_address':'江苏省南京市测试路1号','level':level}]}
def result(pid=''):
 r=place(pid);c=g.candidates_from(geo(),'geocode',r)[0]
 return dict(row=r,query_address=g.query_address(r),candidates=[c],selected_id=c['candidate_id'],reason='测试')
class FakeClient:
 def __init__(self,responses): self.responses=iter(responses)
 def get(self,*args): return next(self.responses)

class GeocodingTests(unittest.TestCase):
 def test_all_101_inputs_have_unique_keys_no_guessed_ids(self):
  rows=g.load_places(g.HERE/'places_input.json')
  self.assertEqual(len(rows),101);self.assertEqual(len({r['record_key'] for r in rows}),101)
  self.assertTrue(all(not r['place_id'] for r in rows))
 def test_coordinate_validation(self):
  for value in ['nan,32','118,inf','181,32','118,91','0,0','oops','118,32,1']:
   self.assertIsNone(g.location(value))
 def test_other_city_rejected_even_if_city_name_conflicts(self):
  data=geo(code='110101');data['geocodes'][0]['city']='南京市'
  self.assertFalse(g.candidates_from(data,'geocode',place())[0]['eligible'])
 def test_coarse_no_sql(self):
  r=g.resolve(place(),FakeClient([geo(level='道路'),{'status':'1','count':'0','pois':[]}]))
  self.assertFalse(r['selected_id']);self.assertEqual(g.generate_sql([r])[1],0)
 def test_unique_precise_candidate_remains_review(self):
  r=g.resolve(place(),FakeClient([geo()]))
  sql,count=g.generate_sql([r]);self.assertEqual(count,1)
  self.assertIn("`location_status` = 'review'",sql)
  self.assertNotIn("`location_status` = 'confirmed'",sql)
 def test_ambiguous_not_first_result(self):
  data=geo();data['geocodes'].append(dict(data['geocodes'][0],location='118.76,32.24'))
  r=g.resolve(place(),FakeClient([data,{'status':'1','count':'0','pois':[]}]))
  self.assertFalse(r['selected_id'])
 def test_sql_id_and_manual_coordinate_guards(self):
  sql,_=g.generate_sql([result('987654321')])
  self.assertIn('WHERE `place_id` = 987654321',sql)
  self.assertIn('`longitude` IS NULL AND `latitude` IS NULL',sql)
  self.assertIn('`location_checked_by` IS NULL',sql)
  self.assertIn('`source_key` =',sql)
  self.assertNotIn('SET @fire_place_id',sql)
 def test_source_key_resolves_real_db_id(self):
  sql,_=g.generate_sql([result()])
  self.assertIn('SET @fire_place_id = (SELECT `place_id`',sql)
  self.assertIn('WHERE `place_id` = @fire_place_id',sql)
  self.assertIn('`address` <=>',sql)
 def test_worker_hard_timeout_and_retry_bound(self):
  with tempfile.TemporaryDirectory() as d:
   args=Namespace(max_seconds=30,offline=False,retries=1,interval=.2,timeout=1)
   client=g.Client('0'*32,Path(d),args)
   with patch.object(g.subprocess,'run',side_effect=subprocess.TimeoutExpired('test',1)) as mock, patch.object(g.time,'sleep'):
    with self.assertRaises(g.RequestFailed): client.get('geocode',{'address':'测试','city':'南京市'})
    self.assertEqual(mock.call_count,2)
    self.assertLessEqual(mock.call_args.kwargs['timeout'],3)
 def test_wrong_key_platform_aborts_immediately(self):
  with tempfile.TemporaryDirectory() as d:
   args=Namespace(max_seconds=30,offline=False,retries=1,interval=1,timeout=1)
   client=g.Client('0'*32,Path(d),args)
   output=Namespace(stdout=json.dumps({'ok':True,'data':{'status':'0','infocode':'10009'}}))
   with patch.object(g.subprocess,'run',return_value=output) as mock:
    with self.assertRaises(g.StopRun): client.get('geocode',{})
    self.assertEqual(mock.call_count,1)
 def test_resume_uses_cache_without_network(self):
  with tempfile.TemporaryDirectory() as d:
   args=Namespace(max_seconds=30,offline=False,retries=0,interval=1,timeout=1)
   client=g.Client('0'*32,Path(d),args)
   output=Namespace(stdout=json.dumps({'ok':True,'data':geo()}))
   with patch.object(g.subprocess,'run',return_value=output): client.get('geocode',{'address':'测试'})
   args.offline=True
   cached=g.Client('',Path(d),args)
   with patch.object(g.subprocess,'run',side_effect=AssertionError('Network forbidden')):
    self.assertEqual(cached.get('geocode',{'address':'测试'}),geo())
   self.assertNotIn('0'*32,(Path(d)/'cache.json').read_text('utf-8'))
 def test_csv_precision_and_duplicate_id_validation(self):
  with tempfile.TemporaryDirectory() as d:
   path=Path(d)/'input.csv';path.write_text('place_id,name,address\n9007199254740993,测试,测试路\n',encoding='utf-8-sig')
   self.assertEqual(g.load_places(path)[0]['place_id'],'9007199254740993')
   path.write_text('place_id,name,address\n1.0,测试,测试路\n',encoding='utf-8-sig')
   with self.assertRaises(ValueError):g.load_places(path)
 def test_poi_result_truncation_not_auto_selected(self):
  poi={'status':'1','count':'9','pois':[{'id':'x','name':'测试机构','location':'118.75,32.23','adcode':'320116'}]}
  r=g.resolve(place(),FakeClient([geo(level='道路'),poi]))
  self.assertFalse(r['selected_id'])
 def test_sql_text_handles_untrusted_quotes_and_newlines(self):
  value="测试'\\\n; DROP TABLE fire_place;--"
  literal=g.sql_text(value)
  self.assertNotIn('DROP TABLE',literal)
  self.assertIn(value.encode().hex(),literal)

 def test_offline_end_to_end_export_and_review(self):
  with tempfile.TemporaryDirectory() as d:
   folder=Path(d);inp=folder/'places.json';out=folder/'out'
   inp.write_text(json.dumps([place('12')],ensure_ascii=False),encoding='utf-8')
   argv=['--input',str(inp),'--output',str(out),'--offline','--limit','1']
   with patch.object(g.Client,'get',return_value=geo()),patch.object(g.subprocess,'run',side_effect=AssertionError('Network forbidden')),patch('sys.stdout',new=io.StringIO()):
    self.assertEqual(g.main(argv),0)
    self.assertEqual(g.main(argv+['--review-csv',str(out/'candidates.csv')]),0)
   self.assertIn('WHERE `place_id` = 12',(out/'update_coordinates.sql').read_text('utf-8'))
   self.assertIn('WHERE `place_id` = 12',(out/'update_coordinates_reviewed.sql').read_text('utf-8'))
   self.assertEqual(json.loads((out/'summary_reviewed.json').read_text('utf-8'))['sql_candidates'],1)
if __name__=='__main__':unittest.main()