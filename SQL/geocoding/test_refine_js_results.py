import copy,importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('refine',Path(__file__).with_name('refine_js_results.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def candidate(cid,address,level='门址',x=118.75,y=32.23):
 return dict(candidate_id=cid,matched_address=address,level=level,longitude=x,latitude=y,method='amap_geocode',eligible=True,in_nanjing=True,name=address,poi_id='',adcode='320116')
def result(candidates):
 return dict(row=dict(name='测试养老院',address='旭东路535号',normalized_address=''),candidates=candidates,selected_id='',reason='')
class RefinementTests(unittest.TestCase):
 def test_house_number_selects_second_not_first(self):
  r=result([candidate('wrong','江苏省南京市六合区旭东路279号'),candidate('right','江苏省南京市六合区旭东路535号')])
  self.assertEqual(m.refine(r)['selected_id'],'right')
 def test_same_address_prefers_level_not_order(self):
  r=result([candidate('number','旭东路535号',level='门牌号'),candidate('door','旭东路535号',x=118.7501)])
  self.assertEqual(m.refine(r)['selected_id'],'door')
 def test_distant_same_address_not_assumed_same_point(self):
  r=result([candidate('one','旭东路535号'),candidate('two','旭东路535号',x=118.77)])
  self.assertFalse(m.refine(r)['selected_id'])
 def test_same_level_ambiguous_not_selected(self):
  r=result([candidate('one','旭东路535号'),candidate('two','旭东路535号',x=118.7501)])
  self.assertFalse(m.refine(r)['selected_id'])
 def test_near_poi_requires_name_evidence(self):
  base=candidate('road','旭东路',level='道路');base['eligible']=False
  poi=candidate('poi','旭东路535号');poi.update(method='amap_poi',name='别的养老院',in_nanjing=False,eligible=False)
  self.assertFalse(m.refine(result([base,poi]))['selected_id'])
 def test_aliases_remove_only_known_region_prefix(self):
  self.assertTrue(m.aliases('南京市江北新区润源养老院')&m.aliases('江北新区润源养老院'))
  self.assertFalse(m.aliases('测试养老院')&m.aliases('测试养老院南分院'))
if __name__=='__main__':unittest.main()