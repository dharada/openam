/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 16:51
 * To change this template use File | Settings | File Templates.
 */

Ext.define('AM.controller.Global', {
    extend: 'Ext.app.Controller',

    models: ['Service'] ,
    stores: [
        'ServiceStore',
        'ConfigStore',
        'RealmStore'
    ],
    views: [
        'ServiceList',
        'ServiceEditor',
        'GlobalEditor'
    ],
    serviceName: '',
    serviceScope:'global',

    init: function() {
        console.log('initialized Global Controller');
        this.control({
            'panel' : {
                 render: this.onPanelRendered
            },
            'servicelist': {
                itemdblclick: this.loadConfigEditor
            }
        })
    },
    loadConfigEditor: function(list, record){
        var cEditor = Ext.widget('serviceeditor');
        var x = list.store['storeName'];
        if (x == 'service')  {
            this.serviceName = record.get('name') ;
        }   else {
            var y = record.get('name');
            if (y == 'GLOBAL') y = 'global';
            if (y == 'TopLevel')   y = 'org';
            this.serviceScope = y;
        };

        cEditor.initServiceEditor(this.serviceName,this.serviceScope);
    } ,
    onPanelRendered: function() {
          console.log("Rendered panel")
    }
})





